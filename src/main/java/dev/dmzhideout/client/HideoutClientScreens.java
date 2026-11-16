package dev.dmzhideout.client;

import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.block.HideoutBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only menu screens, safely absent from a dedicated server. */
@Mod.EventBusSubscriber(modid=DMZHideout.MOD_ID,
        bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class HideoutClientScreens {
    private HideoutClientScreens() { }

    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(HideoutBlocks.STASH_MENU.get(), StashScreen::new);
        event.register(HideoutBlocks.MED_MENU.get(), MedScreen::new);
        event.register(HideoutBlocks.TRADE_MENU.get(), TradeScreen::new);
    }
}
