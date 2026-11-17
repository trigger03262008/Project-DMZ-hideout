package dev.dmzloot.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for item registry, weight and future Inventory grid
 * metadata. Loaded from packaged JSON BEFORE registry registration.
 * IDs, max stack size and damage capacity cannot be datapack-reloaded.
 */
public final class ItemCatalog {
    public record Definition(String id,String category,double massKg,int width,int height,
                             int stackSize,int baseCash,String family,int tier,int durability) {
        public boolean tool(){return durability>0;}
    }
    private static final Map<String,Definition> ITEMS=load();
    private ItemCatalog(){}

    private static Map<String,Definition> load() {
        try(InputStream stream=ItemCatalog.class.getResourceAsStream(
                "/data/dmzloot/catalog/items.json")){
            if(stream==null)throw new IllegalStateException("Missing Loot item catalog");
            JsonArray json=JsonParser.parseReader(
                    new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();
            if(json.size()>512||json.isEmpty())
                throw new IllegalArgumentException("Expected 1..512 Loot entries");
            Map<String,Definition> defs=new LinkedHashMap<>();
            Set<String> categories=Set.of("scrap","parts","electronics","junk","tools","valuables");
            for(JsonElement element:json){
                JsonObject o=element.getAsJsonObject();
                String id=o.get("id").getAsString();
                if(!id.matches("[a-z0-9_]+")||defs.containsKey(id))
                    throw new IllegalArgumentException("Invalid or duplicate Loot ID "+id);
                String category=o.get("category").getAsString();
                if(!categories.contains(category))throw new IllegalArgumentException(category);
                double mass=o.get("mass_kg").getAsDouble();
                JsonArray grid=o.getAsJsonArray("grid");
                int width=grid.get(0).getAsInt(),height=grid.get(1).getAsInt();
                int stack=o.get("stack_size").getAsInt();
                int cash=o.get("base_cash").getAsInt();
                String family=o.has("tool_family")?o.get("tool_family").getAsString():"";
                int tier=o.has("tier")?o.get("tier").getAsInt():-1;
                int durability=o.has("max_durability")?o.get("max_durability").getAsInt():0;
                if(!Double.isFinite(mass)||mass<0||mass>1000||
                        width<1||width>8||height<1||height>8||
                        stack<1||stack>64||cash<0||cash>100000||
                        durability<0||durability>10000)
                    throw new IllegalArgumentException("Invalid Loot stats: "+id);
                if(durability>0 && (!category.equals("tools")||family.isBlank()||
                        tier<0||tier>2||stack!=1))
                    throw new IllegalArgumentException("Invalid tool: "+id);
                defs.put(id,new Definition(id,category,mass,width,height,stack,
                        cash,family,tier,durability));
            }
            return Collections.unmodifiableMap(defs);
        }catch(Exception e){
            throw new IllegalStateException("Cannot initialize Project DMZ Loot catalog",e);
        }
    }
    public static Definition get(String id){return ITEMS.get(id);}
    public static List<Definition> all(){return List.copyOf(ITEMS.values());}
}
