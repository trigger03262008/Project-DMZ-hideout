package dev.dmzhideout.med;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.api.StashApi;
import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.network.HideoutNetwork;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/** JSON-driven medical item crafting. Loot recipe packs can be added later. */
@Mod.EventBusSubscriber(modid=DMZHideout.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class MedRecipes {
    public record Ingredient(String itemId,int count) { }
    public record Recipe(String id,String label,String output,int count,
                         List<Ingredient> ingredients) { }
    private static final Logger LOG=LogUtils.getLogger();
    private static volatile Map<String,Recipe> RECIPES=Map.of();
    private MedRecipes() { }

    @SubscribeEvent public static void onAddReload(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(),"med_recipes") {
            @Override protected void apply(Map<ResourceLocation,JsonElement> files,
                                            ResourceManager manager,ProfilerFiller profiler) {
                Map<String,Recipe> next=new LinkedHashMap<>();
                files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    try {
                        JsonObject json=entry.getValue().getAsJsonObject();
                        if(json.has("enabled")&&!json.get("enabled").getAsBoolean())return;
                        if(json.has("required_mods")) for(JsonElement m:json.getAsJsonArray("required_mods"))
                            if(!ModList.get().isLoaded(m.getAsString()))return;
                        if(next.size()>=64)throw new IllegalArgumentException("Too many recipes");
                        String id=json.has("id")?json.get("id").getAsString():
                                entry.getKey().toString();
                        if(!id.equals(entry.getKey().toString()))
                            throw new IllegalArgumentException("Recipe id/file mismatch");
                        String output=json.get("output").getAsString();
                        Item item=known(output);
                        int count=json.has("count")?json.get("count").getAsInt():1;
                        if(count<1||count>item.getMaxStackSize())
                            throw new IllegalArgumentException("Invalid output count");
                        Map<String,Integer> required=new LinkedHashMap<>();
                        for(JsonElement ingredient:json.getAsJsonArray("ingredients")){
                            JsonObject it=ingredient.getAsJsonObject();
                            String name=it.get("item").getAsString();
                            known(name);
                            int amount=it.get("count").getAsInt();
                            if(amount<1||amount>4096)throw new IllegalArgumentException("Invalid amount");
                            required.merge(name,amount,Integer::sum);
                        }
                        if(required.isEmpty()||required.size()>16)
                            throw new IllegalArgumentException("Recipe needs 1..16 ingredients");
                        List<Ingredient> ingredients=new ArrayList<>();
                        required.forEach((key,value)->ingredients.add(new Ingredient(key,value)));
                        next.put(id,new Recipe(id,
                                json.has("label")?json.get("label").getAsString():output,
                                output,count,List.copyOf(ingredients)));
                    }catch(RuntimeException ex){
                        LOG.error("Invalid medical recipe {}: {}",entry.getKey(),ex.toString());
                    }
                });
                RECIPES=java.util.Collections.unmodifiableMap(new LinkedHashMap<>(next));
                var server=ServerLifecycleHooks.getCurrentServer();
                if(server!=null)server.execute(()->server.getPlayerList().getPlayers()
                        .forEach(HideoutNetwork::sendRecipes));
            }
        });
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)
            HideoutNetwork.sendRecipes(player);
    }

    private static Item known(String id) {
        ResourceLocation key=ResourceLocation.tryParse(id);
        if(key==null||!ForgeRegistries.ITEMS.containsKey(key))
            throw new IllegalArgumentException("Unknown item "+id);
        Item item=ForgeRegistries.ITEMS.getValue(key);
        if(item==null||item==Items.AIR)throw new IllegalArgumentException("Invalid item "+id);
        return item;
    }

    public static JsonArray snapshot() {
        JsonArray list=new JsonArray();
        for(Recipe r:RECIPES.values()){
            JsonObject j=new JsonObject();
            j.addProperty("id",r.id());
            j.addProperty("label",r.label());
            j.addProperty("output",r.output());
            j.addProperty("count",r.count());
            JsonArray required=new JsonArray();
            for(Ingredient ingredient:r.ingredients()){
                JsonObject item=new JsonObject();
                item.addProperty("item",ingredient.itemId());
                item.addProperty("count",ingredient.count());
                required.add(item);
            }
            j.add("ingredients",required);
            list.add(j);
        }
        return list;
    }

    public static boolean craft(ServerPlayer player,BlockPos center,String recipeId,
                                boolean inputStash,boolean outputStash) {
        if(!player.serverLevel().getBlockState(center).is(HideoutBlocks.MED_STATION.get()))
            return false;
        if(player.distanceToSqr(center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5)>64)
            return false;
        Recipe r=RECIPES.get(recipeId);
        if(r==null)return false;
        BlockPos stash=StashApi.findAdjacent(player.serverLevel(),center,player);
        if((inputStash||outputStash)&&stash==null)return false;
        ItemStack result=new ItemStack(known(r.output()),r.count());
        if(outputStash) {
            if(!StashApi.canInsert(player.serverLevel(),stash,player,result))return false;
        } else if(player.getInventory().getFreeSlot()<0) return false;

        for(Ingredient i:r.ingredients()){
            Item item=known(i.itemId());
            int present=0;
            if(inputStash)present=StashApi.count(player.serverLevel(),stash,player,
                    s->s.is(item));
            else for(ItemStack stack:player.getInventory().items)
                if(stack.is(item))present+=stack.getCount();
            if(present<i.count())return false;
        }

        // Back up both inventories before any mutation. A failed insertion
        // or item-handler exception cannot silently consume materials.
        List<ItemStack> playerBefore=new ArrayList<>();
        player.getInventory().items.forEach(stack->playerBefore.add(stack.copy()));
        List<ItemStack> stashBefore=stash==null?List.of():
                StashApi.snapshot(player.serverLevel(),stash,player);
        try {
            for(Ingredient i:r.ingredients()){
                Item item=known(i.itemId());
                if(inputStash) {
                    if(!StashApi.extract(player.serverLevel(),stash,player,
                            stack->stack.is(item),i.count()))
                        throw new IllegalStateException("Stash ingredient removal failed");
                } else {
                    int remaining=i.count();
                    for(ItemStack stack:player.getInventory().items){
                        if(!stack.is(item))continue;
                        int n=Math.min(remaining,stack.getCount());
                        stack.shrink(n);
                        remaining-=n;
                        if(remaining==0)break;
                    }
                    if(remaining!=0)throw new IllegalStateException("Ingredient removal failed");
                }
            }
            player.getInventory().setChanged();
            boolean delivered=outputStash
                    ?StashApi.insert(player.serverLevel(),stash,player,result)
                    :player.getInventory().add(result);
            if(!delivered)throw new IllegalStateException("Output insertion refused");
            player.displayClientMessage(Component.literal(
                    "医療品を製造しました： "+r.label()),true);
            return true;
        }catch(RuntimeException failed){
            if(stash!=null && !StashApi.restore(player.serverLevel(),stash,player,stashBefore))
                LOG.error("CRITICAL: medical crafting rollback failed at {}",stash);
            for(int n=0;n<playerBefore.size();n++)
                player.getInventory().items.set(n,playerBefore.get(n).copy());
            player.getInventory().setChanged();
            LOG.error("Med Station craft canceled: {}",recipeId,failed);
            return false;
        }
    }
}
