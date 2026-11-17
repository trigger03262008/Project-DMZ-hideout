package dev.dmzhideout.block;

import dev.dmzhideout.DMZHideout;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import dev.dmzhideout.menu.StashMenu;
import dev.dmzhideout.menu.MedMenu;
import dev.dmzhideout.menu.TradeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class HideoutBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DMZHideout.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DMZHideout.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DMZHideout.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, DMZHideout.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DMZHideout.MOD_ID);


    public static final RegistryObject<Block> HIDEOUT_CORE = BLOCKS.register("hideout_core",
            () -> new HideoutCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> HIDEOUT_CORE_ITEM = ITEMS.register("hideout_core",
            () -> new BlockItem(HIDEOUT_CORE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> STASH = BLOCKS.register("stash",
            () -> new FacilityBlock(FacilityBlock.Kind.STASH, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(4.0F, 1000.0F)));
    public static final RegistryObject<Block> MED_STATION = BLOCKS.register("med_station",
            () -> new FacilityBlock(FacilityBlock.Kind.MED, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(4.0F, 1000.0F)));
    public static final RegistryObject<Block> TRADING_CENTER = BLOCKS.register("trading_center",
            () -> new FacilityBlock(FacilityBlock.Kind.TRADE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(4.0F, 1000.0F)));

    public static final RegistryObject<Item> STASH_ITEM = ITEMS.register("stash",
            () -> new BlockItem(STASH.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MED_ITEM = ITEMS.register("med_station",
            () -> new BlockItem(MED_STATION.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TRADING_ITEM = ITEMS.register("trading_center",
            () -> new BlockItem(TRADING_CENTER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> SALVAGE_WORKBENCH =
            BLOCKS.register("salvage_workbench",
                    () -> new SalvageWorkbenchBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL).strength(3.5F, 8.0F)));
    public static final RegistryObject<Item> SALVAGE_WORKBENCH_ITEM =
            ITEMS.register("salvage_workbench",
                    () -> new BlockItem(SALVAGE_WORKBENCH.get(),
                            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<StashBlockEntity>> STASH_ENTITY =
            ENTITIES.register("stash", () -> BlockEntityType.Builder.of(
                    StashBlockEntity::new, STASH.get()).build(null));
    public static final RegistryObject<BlockEntityType<FacilityBlockEntity>> MED_ENTITY =
            ENTITIES.register("med_station", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new FacilityBlockEntity(pos, state, FacilityBlock.Kind.MED),
                    MED_STATION.get()).build(null));
    public static final RegistryObject<BlockEntityType<FacilityBlockEntity>> TRADE_ENTITY =
            ENTITIES.register("trading_center", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new FacilityBlockEntity(pos, state, FacilityBlock.Kind.TRADE),
                    TRADING_CENTER.get()).build(null));

    public static final RegistryObject<MenuType<StashMenu>> STASH_MENU =
            MENUS.register("stash", () -> IForgeMenuType.create(StashMenu::new));
    public static final RegistryObject<MenuType<MedMenu>> MED_MENU =
            MENUS.register("med_station", () -> IForgeMenuType.create(MedMenu::new));
    public static final RegistryObject<MenuType<TradeMenu>> TRADE_MENU =
            MENUS.register("trading_center", () -> IForgeMenuType.create(TradeMenu::new));

    public static final RegistryObject<CreativeModeTab> HIDEOUT_TAB =
            TABS.register("hideout", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dmzhideout"))
                    .icon(() -> new ItemStack(HIDEOUT_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HIDEOUT_CORE_ITEM.get());
                        output.accept(STASH_ITEM.get());
                        output.accept(MED_ITEM.get());
                        output.accept(TRADING_ITEM.get());
                        output.accept(SALVAGE_WORKBENCH_ITEM.get());
                    })
                    .build());

    private HideoutBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
