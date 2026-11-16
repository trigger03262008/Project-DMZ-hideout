package dev.dmzhideout.block;

import dev.dmzhideout.menu.StashMenu;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Real, persistent ItemStacks (including NBT) are stored here, independently
 * from Inventory's future 2D placement geometry.
 */
public final class StashBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS_PER_LEVEL = 270;
    public static final int MAX_LEVEL = 5;
    public static final int MAX_SLOTS = SLOTS_PER_LEVEL * MAX_LEVEL;
    private UUID owner;
    private String team = "";
    private boolean shared;
    private int tier = 1;

    private final ItemStackHandler storage = new ItemStackHandler(MAX_SLOTS) {
        @Override protected void onContentsChanged(int slot) {
            StashBlockEntity.this.setChanged();
        }
    };

    public StashBlockEntity(BlockPos pos, BlockState state) {
        super(HideoutBlocks.STASH_ENTITY.get(), pos, state);
    }

    public void establishOwner(ServerPlayer player) {
        if (owner == null) {
            owner = player.getUUID();
            setChanged();
        }
    }

    public boolean hasOwner() { return owner != null; }
    public boolean isOwner(ServerPlayer player) {
        return owner != null && owner.equals(player.getUUID());
    }
    public boolean isShared() { return shared; }
    public int tier() { return tier; }
    public int capacity() { return tier * SLOTS_PER_LEVEL; }
    public ItemStackHandler storage() { return storage; }

    public boolean canAccess(ServerPlayer player) {
        return isOwner(player) || shared && !team.isEmpty()
                && player.getTeam() != null && team.equals(player.getTeam().getName());
    }

    public boolean isEmpty() {
        for (int i = 0; i < capacity(); i++)
            if (!storage.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    public boolean toggleShared(ServerPlayer player) {
        if (!isOwner(player)) return false;
        if (!shared) {
            if (player.getTeam() == null) return false;
            team = player.getTeam().getName();
            shared = true;
        } else {
            shared = false;
            team = "";
        }
        setChanged();
        return true;
    }

    /** An intentionally simple, standalone upgrade path: no PDA or Loot needed. */
    public boolean upgrade(ServerPlayer player) {
        if (!isOwner(player) || tier >= MAX_LEVEL) return false;
        int cost = tier * 16;
        int available = 0;
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(Items.IRON_INGOT)) available += stack.getCount();
        if (!player.getAbilities().instabuild && available < cost) return false;
        if (!player.getAbilities().instabuild) {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.is(Items.IRON_INGOT)) continue;
                int consumed = Math.min(cost, stack.getCount());
                stack.shrink(consumed);
                cost -= consumed;
                if (cost == 0) break;
            }
            player.getInventory().setChanged();
        }
        tier++;
        setChanged();
        return true;
    }

    /** Simulate against a separate handler first, then commit on server thread. */
    public ItemStack insert(ItemStack original, boolean simulate) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        ItemStackHandler target = storage;
        if (simulate) {
            target = new ItemStackHandler(MAX_SLOTS);
            target.deserializeNBT(storage.serializeNBT());
        }
        ItemStack remaining = original.copy();
        for (int i = 0; i < capacity() && !remaining.isEmpty(); i++)
            remaining = target.insertItem(i, remaining, false);
        return remaining;
    }

    public int countMatching(Predicate<ItemStack> matcher) {
        int count = 0;
        for (int i = 0; i < capacity(); i++) {
            ItemStack stack = storage.getStackInSlot(i);
            if (!stack.isEmpty() && matcher.test(stack)) count += stack.getCount();
        }
        return count;
    }

    public boolean extractMatching(Predicate<ItemStack> matcher, int amount) {
        if (amount <= 0 || countMatching(matcher) < amount) return false;
        int left = amount;
        for (int i = 0; i < capacity() && left > 0; i++) {
            ItemStack stack = storage.getStackInSlot(i);
            if (stack.isEmpty() || !matcher.test(stack)) continue;
            int take = Math.min(left, stack.getCount());
            storage.extractItem(i, take, false);
            left -= take;
        }
        return left == 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dmzhideout.stash");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StashMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        team = tag.getString("SharedTeam");
        shared = tag.getBoolean("Shared");
        tier = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Tier")));
        if (tag.contains("Items", net.minecraft.nbt.Tag.TAG_COMPOUND))
            storage.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("SharedTeam", team);
        tag.putBoolean("Shared", shared);
        tag.putInt("Tier", tier);
        tag.put("Items", storage.serializeNBT());
    }
}
