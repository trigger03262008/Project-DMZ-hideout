package dev.dmzloot;

import dev.dmzloot.item.LootItems;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/** Standalone loot and portable salvage module. Hideout is optional. */
@Mod(DMZLoot.ID)
public final class DMZLoot {
    public static final String ID="dmzloot";
    public DMZLoot() {
        LootItems.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
