package dev.dmzhideout.block;

import dev.dmzhideout.DMZHideout;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class HideoutBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DMZHideout.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DMZHideout.MOD_ID);

    public static final RegistryObject<Block> HIDEOUT_CORE = BLOCKS.register("hideout_core",
            () -> new HideoutCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> HIDEOUT_CORE_ITEM = ITEMS.register("hideout_core",
            () -> new BlockItem(HIDEOUT_CORE.get(), new Item.Properties().stacksTo(1)));

    private HideoutBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(HideoutBlocks::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
            event.accept(HIDEOUT_CORE_ITEM.get());
    }
}
