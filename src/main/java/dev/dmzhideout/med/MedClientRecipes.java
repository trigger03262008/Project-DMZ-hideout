package dev.dmzhideout.med;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Client-only read-only display list, always sent by the server. */
public final class MedClientRecipes {
    private static List<JsonObject> recipes=List.of();
    private MedClientRecipes() { }
    public static void update(JsonArray array) {
        List<JsonObject> next=new ArrayList<>();
        for(JsonElement entry:array)next.add(entry.getAsJsonObject().deepCopy());
        recipes=List.copyOf(next);
    }
    public static List<JsonObject> list() { return recipes; }
    public static void clear() { recipes=List.of(); }
}
