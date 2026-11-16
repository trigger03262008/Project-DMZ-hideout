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
    public record Recipe(String id,String label,String category,String output,int count,
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
                                json.has("category")?json.get("category").getAsString():"other",
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
            j.addProperty("category",r.category());
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


    /** Server-authoritative availability for tooltips; locked stacks are excluded. */
    public static JsonObject stashStock(ServerPlayer player,BlockPos center) {
        JsonObject counts=new JsonObject();
        BlockPos pos=StashApi.findAdjacent(player.serverLevel(),center,player);
        for(Recipe recipe:RECIPES.values())for(Ingredient ing:recipe.ingredients()){
            if(counts.has(ing.itemId()))continue;
            int qty=pos==null?0:StashApi.count(player.serverLevel(),pos,player,
                    stack->stack.is(known(ing.itemId())));
            counts.addProperty(ing.itemId(),qty);
        }
        return counts;
    }

    /**
     * Instant crafting: locked Stash stock is excluded. Materials come from
     * the adjacent Stash FIRST, then the player's vanilla inventory. The
     * destination prefers the Stash (if selected) but can safely fall back.
     */
    public static boolean craft(ServerPlayer player,BlockPos center,String recipeId,
                                boolean stashFirst,boolean outputStashFirst) {
        if(!player.serverLevel().getBlockState(center).is(HideoutBlocks.MED_STATION.get()))
            return false;
        if(player.distanceToSqr(center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5)>64)
            return false;
        Recipe recipe=RECIPES.get(recipeId);
        if(recipe==null)return false;
        BlockPos stash=StashApi.findAdjacent(player.serverLevel(),center,player);
        ItemStack output=new ItemStack(known(recipe.output()),recipe.count());

        // Authoritative aggregate preflight across BOTH inventory sources.
        for(Ingredient ing:recipe.ingredients()){
            Item ingredient=known(ing.itemId());
            int fromStash=stash==null?0:StashApi.count(player.serverLevel(),stash,player,
                    stack->stack.is(ingredient));
            int fromPlayer=0;
            for(ItemStack stack:player.getInventory().items)
                if(stack.is(ingredient))fromPlayer+=stack.getCount();
            if(fromStash+fromPlayer<ing.count())return false;
        }
        List<ItemStack> playerBefore=new ArrayList<>();
        player.getInventory().items.forEach(stack->playerBefore.add(stack.copy()));
        List<ItemStack> stashBefore=stash==null?List.of():
                StashApi.snapshot(player.serverLevel(),stash,player);
        try{
            for(Ingredient ing:recipe.ingredients()){
                Item ingredient=known(ing.itemId());
                int need=ing.count();
                if(stashFirst && stash!=null){
                    int take=Math.min(need,StashApi.count(player.serverLevel(),stash,player,
                            stack->stack.is(ingredient)));
                    if(take>0){
                        if(!StashApi.extract(player.serverLevel(),stash,player,
                                stack->stack.is(ingredient),take))
                            throw new IllegalStateException("Stash extraction refused");
                        need-=take;
                    }
                }
                for(ItemStack stack:player.getInventory().items){
                    if(need==0)break;
                    if(!stack.is(ingredient))continue;
                    int take=Math.min(need,stack.getCount());
                    stack.shrink(take);
                    need-=take;
                }
                if(need>0 && !stashFirst && stash!=null){
                    if(!StashApi.extract(player.serverLevel(),stash,player,
                            stack->stack.is(ingredient),need))
                        throw new IllegalStateException("Fallback Stash extraction refused");
                    need=0;
                }
                if(need>0)throw new IllegalStateException("Insufficient inputs after preflight");
            }
            player.getInventory().setChanged();
            boolean delivered=false;
            if(outputStashFirst && stash!=null)
                delivered=StashApi.insert(player.serverLevel(),stash,player,output);
            if(!delivered)delivered=player.getInventory().add(output.copy());
            if(!delivered && !outputStashFirst && stash!=null)
                delivered=StashApi.insert(player.serverLevel(),stash,player,output);
            if(!delivered)throw new IllegalStateException("Both output destinations full");
            player.displayClientMessage(Component.literal("医療品を製造しました："+
                    recipe.label()),true);
            HideoutNetwork.sendMedStock(player,center);
            return true;
        }catch(RuntimeException error){
            if(stash!=null && !StashApi.restore(
                    player.serverLevel(),stash,player,stashBefore))
                LOG.error("CRITICAL: Med Station Stash rollback failed at {}",stash);
            for(int i=0;i<playerBefore.size();i++)
                player.getInventory().items.set(i,playerBefore.get(i).copy());
            player.getInventory().setChanged();
            LOG.error("Med Station crafting transaction rolled back: {}",recipeId,error);
            return false;
        }
    }
}
