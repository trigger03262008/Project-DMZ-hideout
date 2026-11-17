package dev.dmzloot.salvage;

import com.mojang.logging.LogUtils;
import dev.dmzloot.item.ItemCatalog;
import dev.dmzloot.item.LootItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Server-only atomic disassembly. Source is held junk; outputs go to normal
 * inventory. Insufficient destination space restores every original slot.
 * No item drops are silently destroyed by a full inventory.
 */
public final class SalvageService {
    private static final Logger LOG=LogUtils.getLogger();
    private SalvageService(){}

    private static void message(ServerPlayer player,String msg){
        player.displayClientMessage(Component.literal(msg),true);
    }
    private static boolean benchValid(ServerPlayer p,BlockPos at){
        if(at==null||!p.serverLevel().hasChunkAt(at)||
                p.distanceToSqr(at.getX()+0.5,at.getY()+0.5,at.getZ()+0.5)>64)
            return false;
        ResourceLocation id=ForgeRegistries.BLOCKS.getKey(
                p.serverLevel().getBlockState(at).getBlock());
        return new ResourceLocation("dmzhideout","salvage_workbench").equals(id);
    }
    private record Pick(ItemStack stack,int wear){}
    private static Pick pickTool(Inventory inv,SalvageRecipes.Tool required){
        Pick selected=null;
        for(ItemStack item:inv.items){
            if(!(item.getItem() instanceof LootItem tool))continue;
            ItemCatalog.Definition def=tool.definition();
            if(!required.family().equals(def.family())||def.tier()<required.tier())continue;
            // Higher grade is more durable, but don't waste it if a standard
            // tool with enough durability is already available.
            int cost=(int)Math.ceil(required.wear()*
                    (def.tier()==0?1.5:def.tier()==2?0.5:1.0));
            if(item.getMaxDamage()-item.getDamageValue()<cost)continue;
            if(selected==null||
                    ((LootItem)selected.stack().getItem()).definition().tier()>def.tier())
                selected=new Pick(item,cost);
        }
        return selected;
    }
    private static List<ItemStack> snapshot(Inventory inv){
        List<ItemStack> old=new ArrayList<>(inv.items.size());
        for(ItemStack stack:inv.items)old.add(stack.copy());
        return old;
    }
    private static void restore(Inventory inv,List<ItemStack> old){
        for(int i=0;i<old.size();i++)inv.items.set(i,old.get(i).copy());
        inv.setChanged();
    }

    /** @param bench null = portable (sneak + right click) */
    public static boolean salvage(ServerPlayer player,BlockPos bench){
        if(bench!=null&&!benchValid(player,bench))return false;
        ItemStack held=player.getMainHandItem();
        SalvageRecipes.Recipe recipe=SalvageRecipes.forStack(held);
        if(recipe==null)return false;
        if(bench==null&&recipe.tableOnly()){
            message(player,"このジャンクはワークベンチが必要です");
            return false;
        }
        Pick pick=bench!=null?null:pickTool(player.getInventory(),recipe.tool());
        if(bench==null&&pick==null){
            message(player,"対応工具がないか、工具の耐久が不足しています");
            return false;
        }
        Inventory inv=player.getInventory();
        List<ItemStack> old=snapshot(inv);
        try {
            held.shrink(1);
            for(SalvageRecipes.Output out:recipe.outputs()){
                ItemStack result=new ItemStack(out.item(),out.count());
                inv.add(result);
                if(!result.isEmpty())throw new IllegalStateException("Not enough inventory space");
            }
            if(pick!=null){
                ItemStack tool=pick.stack();
                // No hurtAndBreak: durability 0 means an inert, REPAIRABLE tool.
                tool.setDamageValue(Math.min(tool.getMaxDamage(),
                        tool.getDamageValue()+pick.wear()));
            }
            inv.setChanged();
            message(player,bench==null?"現地解体完了（工具耐久消費）":
                    "ワークベンチ解体完了（工具耐久消費なし）");
            return true;
        }catch(RuntimeException ex){
            restore(inv,old);
            message(player,"収納先が不足、または解体に失敗しました");
            LOG.warn("Loot salvage rolled back for {}: {}",player.getScoreboardName(),ex.toString());
            return false;
        }
    }

    private static Map<Item,Integer> repairMaterials(ItemCatalog.Definition def){
        Map<Item,Integer> map=new LinkedHashMap<>();
        switch(def.family()){
            case "screwdriver" -> {
                put(map,"scrap_iron",1);put(map,"scrap_plastic",1);put(map,"screw",1);
            }
            case "wrench" -> {put(map,"scrap_iron",2);put(map,"bolt",1);}
            case "pliers" -> {put(map,"scrap_iron",1);put(map,"spring",1);}
            case "sewing_kit" -> {put(map,"scrap_cloth",2);}
            default -> throw new IllegalArgumentException("Unknown tool family");
        }
        if(def.tier()==2)put(map,"insulation_tape",1);
        return map;
    }
    private static void put(Map<Item,Integer> m,String id,int count){
        Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("dmzloot",id));
        if(item==null)throw new IllegalStateException("Unknown material "+id);
        m.merge(item,count,Integer::sum);
    }
    private static int count(Inventory inv,Item item){
        int n=0;
        for(ItemStack stack:inv.items)if(stack.is(item))n+=stack.getCount();
        return n;
    }
    private static boolean repair(ServerPlayer player) {
        ItemStack tool=player.getMainHandItem();
        if(!(tool.getItem() instanceof LootItem loot)||!loot.definition().tool())
            return false;
        if(tool.getDamageValue()<=0){
            message(player,"工具は既に最大耐久です");
            return true;
        }
        Inventory inv=player.getInventory();
        Map<Item,Integer> materials=repairMaterials(loot.definition());
        for(var entry:materials.entrySet())if(count(inv,entry.getKey())<entry.getValue()){
            message(player,"工具の修理素材が不足しています");
            return true;
        }
        List<ItemStack> old=snapshot(inv);
        try {
            for(var entry:materials.entrySet()){
                int remaining=entry.getValue();
                for(ItemStack stack:inv.items){
                    if(remaining<=0)break;
                    if(!stack.is(entry.getKey()))continue;
                    int used=Math.min(stack.getCount(),remaining);
                    stack.shrink(used);
                    remaining-=used;
                }
                if(remaining>0)throw new IllegalStateException("Material changed");
            }
            int restored=Math.max(1,(int)Math.ceil(tool.getMaxDamage()*0.4));
            tool.setDamageValue(Math.max(0,tool.getDamageValue()-restored));
            inv.setChanged();
            message(player,"工具を修理しました (+"+restored+" 耐久)");
            return true;
        }catch(RuntimeException e){
            restore(inv,old);
            LOG.warn("Loot tool repair rolled back",e);
            message(player,"工具の修理に失敗しました");
            return false;
        }
    }
    public static boolean useWorkbench(ServerPlayer player,BlockPos at){
        if(!benchValid(player,at))return false;
        ItemStack stack=player.getMainHandItem();
        if(SalvageRecipes.forStack(stack)!=null)return salvage(player,at);
        if(stack.getItem() instanceof LootItem item&&item.definition().tool())
            return repair(player);
        message(player,"ジャンクまたは修理したい工具をメインハンドに持って使用してください");
        return true;
    }
}
