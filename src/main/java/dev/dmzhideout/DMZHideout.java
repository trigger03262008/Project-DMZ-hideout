package dev.dmzhideout;

import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.config.HideoutConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/** Independent Project DMZ Hideout module. No dependency on PDA, Health or Inventory. */
@Mod(DMZHideout.MOD_ID)
public final class DMZHideout {
    public static final String MOD_ID = "dmzhideout";

    public DMZHideout() {
        HideoutBlocks.register(FMLJavaModLoadingContext.get().getModEventBus());
        HideoutNetwork.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HideoutConfig.SPEC);
    }
}
