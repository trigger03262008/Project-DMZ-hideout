package dev.dmzhideout.menu;

import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.compat.MedicalCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class MedMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final SimpleContainerData quote = new SimpleContainerData(2);
    private final Inventory inv;

    public MedMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }
    public MedMenu(int id, Inventory inv, BlockPos pos) {
        super(HideoutBlocks.MED_MENU.get(), id);
        this.inv = inv;
        this.pos = pos;
        addDataSlots(quote);
    }
    public BlockPos pos() { return pos; }
    public int quote() { return (quote.get(0) & 65535) | (quote.get(1) << 16); }

    @Override public void broadcastChanges() {
        if (inv.player instanceof ServerPlayer player) {
            int amount=MedicalCompat.quote(player);
            quote.set(0,amount & 65535);
            quote.set(1,(amount >>> 16) & 65535);
            if (player.tickCount % 20 == 0)
                dev.dmzhideout.network.HideoutNetwork.sendMedStock(player,pos);
        }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(HideoutBlocks.MED_STATION.get())
                && player.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
