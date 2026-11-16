package dev.dmzhideout.block;

import dev.dmzhideout.menu.MedMenu;
import dev.dmzhideout.menu.TradeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FacilityBlockEntity extends BlockEntity implements MenuProvider {
    private final FacilityBlock.Kind kind;

    public FacilityBlockEntity(BlockPos pos, BlockState state, FacilityBlock.Kind kind) {
        super(kind == FacilityBlock.Kind.MED ? HideoutBlocks.MED_ENTITY.get() :
                HideoutBlocks.TRADE_ENTITY.get(), pos, state);
        this.kind = kind;
    }

    @Override public Component getDisplayName() {
        return Component.translatable(kind == FacilityBlock.Kind.MED ?
                "block.dmzhideout.med_station" : "block.dmzhideout.trading_center");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return kind == FacilityBlock.Kind.MED ? new MedMenu(id, inv, getBlockPos()) :
                new TradeMenu(id, inv, getBlockPos());
    }
}
