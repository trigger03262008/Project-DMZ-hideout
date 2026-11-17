package dev.dmzloot.salvage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.dmzloot.DMZLoot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** JSON-powered deterministic disassembly. Only player items, no structure loot. */
@Mod.EventBusSubscriber(modid=DMZLoot.ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class SalvageRecipes {
    public record Output(Item item,int count){}
    public record Tool(String family,int tier,int wear){}
    public record Recipe(ResourceLocation input,Tool tool,
                         List<Output> outputs,boolean tableOnly){}
    private static final Logger LOG=LogUtils.getLogger();
    private static volatile Map<ResourceLocation,Recipe> RECIPES=Map.of();
    private SalvageRecipes(){}
    public static Recipe forStack(ItemStack stack){
        if(stack==null||stack.isEmpty())return null;
        return RECIPES.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event){
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(),"salvage"){
            @Override protected void apply(Map<ResourceLocation,JsonElement> files,
                                            ResourceManager manager,ProfilerFiller profiler){
                Map<ResourceLocation,Recipe> next=new LinkedHashMap<>();
                files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
                    try {
                        if(next.size()>=256)throw new IllegalArgumentException("Recipe cap 256");
                        JsonObject o=entry.getValue().getAsJsonObject();
                        ResourceLocation input=ResourceLocation.tryParse(o.get("item").getAsString());
                        if(!entry.getKey().equals(input))
                            throw new IllegalArgumentException("ID must match JSON path");
                        findItem(input.toString());
                        JsonArray tools=o.getAsJsonArray("tools");
                        // v0.1: one required tool family maximum. A bench skips it.
                        if(tools.size()!=1)throw new IllegalArgumentException("One tool required");
                        JsonObject t=tools.get(0).getAsJsonObject();
                        String family=t.get("family").getAsString();
                        if(!java.util.Set.of("screwdriver","wrench","pliers","sewing_kit")
                                .contains(family))throw new IllegalArgumentException("Bad family");
                        int tier=t.get("min_tier").getAsInt();
                        int wear=t.get("wear").getAsInt();
                        if(tier<0||tier>2||wear<1||wear>50)
                            throw new IllegalArgumentException("Bad tool requirement");
                        JsonArray output=o.getAsJsonArray("outputs");
                        if(output.isEmpty()||output.size()>8)
                            throw new IllegalArgumentException("Expected 1..8 outputs");
                        List<Output> results=new ArrayList<>();
                        for(JsonElement line:output){
                            JsonObject element=line.getAsJsonObject();
                            int count=element.get("count").getAsInt();
                            Item item=findItem(element.get("item").getAsString());
                            if(count<1||count>item.getMaxStackSize())
                                throw new IllegalArgumentException("Bad output count");
                            results.add(new Output(item,count));
                        }
                        next.put(input,new Recipe(input,new Tool(family,tier,wear),
                                List.copyOf(results),o.has("table_only")&&
                                o.get("table_only").getAsBoolean()));
                    }catch(RuntimeException ex){
                        LOG.error("Invalid Loot salvage recipe {}: {}",
                                entry.getKey(),ex.toString());
                    }
                });
                RECIPES=Collections.unmodifiableMap(next);
                LOG.info("Project DMZ Loot: loaded {} salvage recipes",next.size());
            }
        });
    }
    private static Item findItem(String id){
        ResourceLocation key=ResourceLocation.tryParse(id);
        if(key==null||!ForgeRegistries.ITEMS.containsKey(key))
            throw new IllegalArgumentException("Unknown Loot item "+id);
        Item item=ForgeRegistries.ITEMS.getValue(key);
        if(item==null||item==Items.AIR)
            throw new IllegalArgumentException("Invalid Loot item "+id);
        return item;
    }
}
