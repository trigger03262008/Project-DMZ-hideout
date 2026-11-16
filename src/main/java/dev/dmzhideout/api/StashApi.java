package dev.dmzhideout.api;

import dev.dmzhideout.block.StashBlockEntity;
import dev.dmzhideout.block.HideoutBlocks;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Safe, server-thread-only storage bridge for Traders, crafting and future
 * Inventory adapters. Never expose the modifiable handler cross-mod.
 */
public final class StashApi {
    private StashApi() { }

    public static BlockPos findAdjacent(ServerLevel level, BlockPos center,
                                        ServerPlayer player) {
        if (player == null || !level.hasChunkAt(center)) return null;
        for (Direction dir : Direction.values()) {
            BlockPos pos = center.relative(dir);
            if (!level.hasChunkAt(pos)) continue;
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof StashBlockEntity stash && stash.canAccess(player))
                return pos;
        }
        return null;
    }

    private static StashBlockEntity check(ServerLevel level, BlockPos pos,
                                          ServerPlayer player) {
        if (level == null || pos == null || player == null || !level.hasChunkAt(pos))
            return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof StashBlockEntity stash && stash.canAccess(player) ? stash : null;
    }

    public static boolean canInsert(ServerLevel level, BlockPos pos,
                                    ServerPlayer player, ItemStack item) {
        StashBlockEntity stash = check(level, pos, player);
        return stash != null && item != null &&
                stash.insert(item, true).isEmpty();
    }

    public static boolean insert(ServerLevel level, BlockPos pos,
                                 ServerPlayer player, ItemStack item) {
        StashBlockEntity stash = check(level, pos, player);
        if (stash == null || item == null || !canInsert(level, pos, player, item))
            return false;
        return stash.insert(item.copy(), false).isEmpty();
    }

    public static int count(ServerLevel level, BlockPos pos, ServerPlayer player,
                            Predicate<ItemStack> matcher) {
        StashBlockEntity stash = check(level, pos, player);
        return stash == null ? 0 : stash.countMatching(matcher);
    }

    public static boolean extract(ServerLevel level, BlockPos pos,
                                  ServerPlayer player, Predicate<ItemStack> matcher,
                                  int amount) {
        StashBlockEntity stash = check(level, pos, player);
        return stash != null && stash.extractMatching(matcher, amount);
    }

    /**
     * Trusted server-only rollback hook. Used when a multi-source settlement
     * fails; restores precisely one previous snapshot with no dropped items.
     */
    /** Spendable stacks exclude manually locked slots; indices stay stable. */
    public static List<ItemStack> spendableSnapshot(ServerLevel level, BlockPos pos,
                                                    ServerPlayer player) {
        StashBlockEntity stash = check(level, pos, player);
        if (stash == null) return List.of();
        List<ItemStack> spendable = new ArrayList<>(stash.capacity());
        for (int i = 0; i < stash.capacity(); i++) {
            spendable.add(stash.isLocked(i) ? ItemStack.EMPTY :
                    stash.storage().getStackInSlot(i).copy());
        }
        return List.copyOf(spendable);
    }

    public static boolean restore(ServerLevel level, BlockPos pos,
                                  ServerPlayer player, List<ItemStack> previous) {
        StashBlockEntity stash = check(level, pos, player);
        if (stash == null || previous == null || previous.size() != stash.capacity())
            return false;
        for (int i = 0; i < previous.size(); i++) {
            ItemStack stack = previous.get(i);
            if (stack == null || stack.getCount() > stack.getMaxStackSize())
                return false;
        }
        for (int i = 0; i < previous.size(); i++)
            stash.storage().setStackInSlot(i, previous.get(i).copy());
        stash.setChanged();
        return true;
    }

    public static List<ItemStack> snapshot(ServerLevel level, BlockPos pos,
                                            ServerPlayer player) {
        StashBlockEntity stash = check(level, pos, player);
        if (stash == null) return List.of();
        List<ItemStack> stacks = new ArrayList<>(stash.capacity());
        for (int i = 0; i < stash.capacity(); i++)
            stacks.add(stash.storage().getStackInSlot(i).copy());
        return List.copyOf(stacks);
    }
}
