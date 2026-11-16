package dev.dmzhideout.menu;

import dev.dmzhideout.api.StashApi;
import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.block.StashBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

/** Thirty-six read-only stash previews in the right-hand Trading Center panel. */
public final class TradeMenu extends AbstractContainerMenu {
    private final BlockPos center;
    private final Inventory inventory;
    private final SimpleContainerData pageInfo = new SimpleContainerData(3);
    private final int[] mapping = new int[36];
    private final ItemStackHandler mirror = new ItemStackHandler(36);
    private int page, pages=1, revision=-1;
    private String search = "";

    public TradeMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }
    public TradeMenu(int id, Inventory inventory, BlockPos center) {
        super(HideoutBlocks.TRADE_MENU.get(), id);
        this.inventory = inventory;
        this.center = center;
        java.util.Arrays.fill(mapping, -1);
        IItemHandlerModifiable display = new IItemHandlerModifiable() {
            public int getSlots() { return 36; }
            public ItemStack getStackInSlot(int n) {
                StashBlockEntity stash = selectedStash();
                return stash != null && mapping[n] >= 0 ?
                        stash.storage().getStackInSlot(mapping[n]) :
                        mirror.getStackInSlot(n);
            }
            public ItemStack insertItem(int n, ItemStack item, boolean simulate) { return item; }
            public ItemStack extractItem(int n, int count, boolean simulate) { return ItemStack.EMPTY; }
            public int getSlotLimit(int n) { return 64; }
            public boolean isItemValid(int n, ItemStack item) { return false; }
            public void setStackInSlot(int n, ItemStack stack) { mirror.setStackInSlot(n, stack); }
        };
        for (int i=0;i<36;i++) {
            int n=i;
            addSlot(new SlotItemHandler(display,n,306 + (i%6)*18,54+(i/6)*18) {
                @Override public boolean mayPlace(ItemStack item) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        addDataSlots(pageInfo);
        refresh();
    }
    public BlockPos pos() { return center; }
    public int page() { return pageInfo.get(0); }
    public int pages() { return pageInfo.get(1); }
    public boolean hasStash() { return pageInfo.get(2) != 0; }

    public StashBlockEntity selectedStash() {
        if (!(inventory.player instanceof ServerPlayer player)) return null;
        BlockPos stash = StashApi.findAdjacent(player.serverLevel(), center, player);
        if (stash == null) return null;
        return player.level().getBlockEntity(stash) instanceof StashBlockEntity s ? s : null;
    }

    public void search(String text) {
        search = text == null ? "" : text.toLowerCase(Locale.ROOT).strip();
        page=0;refresh();
    }
    public void navigate(int delta) { page=Math.max(0,Math.min(pages-1,page+delta));refresh(); }

    public void refresh() {
        StashBlockEntity stash=selectedStash();
        List<Integer> indexes=new ArrayList<>();
        if (stash != null) {
            for(int i=0;i<stash.capacity();i++){
                ItemStack stack=stash.storage().getStackInSlot(i);
                if (!stack.isEmpty() && (search.isEmpty() ||
                        stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)))
                    indexes.add(i);
            }
        }
        pages=Math.max(1,(indexes.size()+35)/36);
        page=Math.min(page,pages-1);
        for(int i=0;i<36;i++) {
            int n=page*36+i;
            mapping[i] = n < indexes.size() ? indexes.get(n) : -1;
        }
        pageInfo.set(0,page);pageInfo.set(1,pages);pageInfo.set(2,stash==null?0:1);
        revision=stash==null?-1:stash.revision();
        broadcastFullState();
    }
    @Override public void broadcastChanges() {
        StashBlockEntity stash = selectedStash();
        if ((stash==null?-1:stash.revision()) != revision) refresh();
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(center).is(HideoutBlocks.TRADING_CENTER.get()) &&
                player.distanceToSqr(center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5) <=64.0D;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) { return ItemStack.EMPTY; }
}
