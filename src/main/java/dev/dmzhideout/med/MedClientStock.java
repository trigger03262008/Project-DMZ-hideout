package dev.dmzhideout.med;

import com.google.gson.JsonObject;
import dev.dmzhideout.DMZHideout;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lightweight stock totals sent from the server for nearby accessible Stash. */
@Mod.EventBusSubscriber(modid=DMZHideout.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class MedClientStock {
    private static JsonObject stash=new JsonObject();
    private MedClientStock() { }
    public static void update(JsonObject next) { stash=next.deepCopy(); }
    public static int available(String itemId) {
        return stash.has(itemId)?Math.max(0,stash.get(itemId).getAsInt()):0;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        stash=new JsonObject();
    }
}
