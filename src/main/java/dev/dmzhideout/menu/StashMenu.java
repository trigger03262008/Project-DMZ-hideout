package dev.dmzhideout.menu;

import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.block.StashBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

/** Searchable, paginated view over a much larger persisted stash. */
public final class StashMenu extends AbstractContainerMenu {
    public static final int VISIBLE = 54;
    private final Inventory inventory;
    private final BlockPos pos;
    private final StashBlockEntity stash;
    private final ItemStackHandler clientMirror = new ItemStackHandler(VISIBLE);
    private final int[] map = new int[VISIBLE];
    private final SimpleContainerData info = new SimpleContainerData(5 + VISIBLE);
    private int page, pages = 1, sort, filter, version = -1;
    private String query = "";

    public StashMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }
    private StashMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof StashBlockEntity s ? s : null);
    }
    public StashMenu(int id, Inventory inventory, StashBlockEntity stash) {
        this(id, inventory, stash.getBlockPos(), stash);
    }
    private StashMenu(int id, Inventory inventory, BlockPos pos, StashBlockEntity stash) {
        super(HideoutBlocks.STASH_MENU.get(), id);
        this.inventory = inventory;
        this.pos = pos;
        this.stash = stash;
        java.util.Arrays.fill(map, -1);
        IItemHandlerModifiable view = new IItemHandlerModifiable() {
            public int getSlots() { return VISIBLE; }
            private int slotIndex(int n) { return n >= 0 && n < VISIBLE ? map[n] : -1; }
            public ItemStack getStackInSlot(int n) {
                if (!(StashMenu.this.inventory.player instanceof ServerPlayer))
                    return clientMirror.getStackInSlot(n);
                int actual = slotIndex(n);
                return actual < 0 ? ItemStack.EMPTY : StashMenu.this.stash.storage().getStackInSlot(actual);
            }
            public ItemStack insertItem(int n, ItemStack stack, boolean simulate) {
                if (!(StashMenu.this.inventory.player instanceof ServerPlayer) ||
                        StashMenu.this.stash == null) return stack;
                int actual = slotIndex(n);
                return actual < 0 ? stack : StashMenu.this.stash.storage().insertItem(actual, stack, simulate);
            }
            public ItemStack extractItem(int n, int amount, boolean simulate) {
                if (!(StashMenu.this.inventory.player instanceof ServerPlayer) ||
                        StashMenu.this.stash == null) return ItemStack.EMPTY;
                int actual = slotIndex(n);
                return actual < 0 || StashMenu.this.stash.isLocked(actual)
                        ? ItemStack.EMPTY : StashMenu.this.stash.storage()
                                .extractItem(actual, amount, simulate);
            }
            public int getSlotLimit(int n) { return 64; }
            public boolean isItemValid(int n, ItemStack stack) { return true; }
            public void setStackInSlot(int n, ItemStack stack) {
                if (!(StashMenu.this.inventory.player instanceof ServerPlayer))
                    clientMirror.setStackInSlot(n, stack);
                else {
                    int actual = slotIndex(n);
                    if (actual >= 0) StashMenu.this.stash.storage().setStackInSlot(actual, stack);
                }
            }
        };
        for (int i = 0; i < VISIBLE; i++) {
            final int visible = i;
            addSlot(new SlotItemHandler(view, i, 8 + i % 9 * 18, 37 + i / 9 * 18) {
                @Override public boolean mayPickup(Player player) {
                    return StashMenu.this.stash == null ||
                            !(player instanceof ServerPlayer) ||
                            !StashMenu.this.stash.isLocked(map[visible]);
                }
            });
        }
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 156 + y * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventory, i, 8 + i * 18, 214));
        addDataSlots(info);
        rebuild();
    }

    public BlockPos pos() { return pos; }
    public int page() { return info.get(0); }
    public int pages() { return info.get(1); }
    public int tier() { return info.get(2); }
    public boolean shared() { return info.get(3) != 0; }
    public int filterIndex() { return info.get(4); }
    public boolean lockedVisible(int index) {
        return index >= 0 && index < VISIBLE && info.get(5 + index) != 0;
    }
    public void toggleLock(ServerPlayer player, int index) {
        if (stash != null && index >= 0 && index < VISIBLE && map[index] >= 0) {
            stash.toggleLock(player, map[index]);
            rebuild();
        }
    }
    public boolean isOwner(ServerPlayer p) { return stash != null && stash.isOwner(p); }

    public void search(String q) {
        query = q == null ? "" : q.toLowerCase(Locale.ROOT).strip();
        page = 0;
        rebuild();
    }
    public void sort() { sort = (sort + 1) % 3; page = 0; rebuild(); }
    public void filter() { filter = (filter + 1) % 5; page = 0; rebuild(); }
    public void navigate(int delta) {
        page = Math.max(0, Math.min(pages - 1, page + delta));
        rebuild();
    }

    public void updated() { rebuild(); }

    private void rebuild() {
        if (stash == null || !(inventory.player instanceof ServerPlayer)) return;
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < stash.capacity(); i++) {
            ItemStack stack = stash.storage().getStackInSlot(i);
            boolean category = switch (filter) {
                case 1 -> stack.getItem() instanceof BlockItem;
                case 2 -> stack.isEdible();
                case 3 -> stack.getItem() instanceof ArmorItem || stack.isDamageableItem();
                case 4 -> !stack.isEmpty() && "dmzcore".equals(
                        ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace());
                default -> true;
            };
            if (category && (query.isEmpty() || !stack.isEmpty() && (
                    stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()
                    .contains(query)))) indexes.add(i);
        }
        if (sort != 0) {
            Comparator<Integer> comparator = Comparator.comparing(i -> {
                ItemStack s = stash.storage().getStackInSlot(i);
                return s.isEmpty() ? "~" : s.getHoverName().getString().toLowerCase(Locale.ROOT);
            });
            if (sort == 2) comparator = Comparator.comparingInt(
                    (Integer i) -> stash.storage().getStackInSlot(i).getCount()).reversed();
            indexes.sort(comparator);
        }
        pages = Math.max(1, (indexes.size() + VISIBLE - 1) / VISIBLE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < VISIBLE; i++) {
            int n = page * VISIBLE + i;
            map[i] = n < indexes.size() ? indexes.get(n) : -1;
        }
        info.set(0, page);
        info.set(1, pages);
        info.set(2, stash.tier());
        info.set(3, stash.isShared() ? 1 : 0);
        info.set(4, filter);
        for (int i=0;i<VISIBLE;i++)
            info.set(5+i, map[i] >= 0 && stash.isLocked(map[i]) ? 1 : 0);
        version = stash.revision();
        broadcastFullState();
    }

    @Override public void broadcastChanges() {
        if (stash != null && inventory.player instanceof ServerPlayer &&
                version != stash.revision()) rebuild();
        super.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) {
        return stash != null && !stash.isRemoved() &&
                player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0D
                && (!(player instanceof ServerPlayer sp) || stash.canAccess(sp));
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (stash == null || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index < VISIBLE &&
                stash.isLocked(map[index])) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index >= VISIBLE) {
            ItemStack remainder = stash.insert(original, false);
            if (remainder.getCount() == original.getCount()) return ItemStack.EMPTY;
            slot.set(remainder);
            stash.setChanged();
        } else {
            if (!moveItemStackTo(current, VISIBLE, slots.size(), true))
                return ItemStack.EMPTY;
            slot.setChanged();
            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        }
        slot.onTake(player, current);
        return original;
    }
}
