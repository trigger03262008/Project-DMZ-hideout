package dev.dmzhideout.med;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import dev.dmzhideout.DMZHideout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only read-only display list, always sent by the server. */
@Mod.EventBusSubscriber(modid=DMZHideout.MOD_ID,
        bus=Mod.EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
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

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
