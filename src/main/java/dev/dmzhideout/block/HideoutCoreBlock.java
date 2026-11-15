package dev.dmzhideout.block;

import dev.dmzhideout.config.HideoutConfig;
import dev.dmzhideout.zone.SafeZoneSavedData;
import dev.dmzhideout.zone.SafeZoneSavedData.Zone;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** Marker block; server SavedData is the authoritative source of protection. */
public final class HideoutCoreBlock extends Block {
    public HideoutCoreBlock(Properties properties) { super(properties); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);
        if (!(level instanceof ServerLevel server)) return;
        if (!(placer instanceof ServerPlayer player)) {
            // Command/worldgen placement is inert. Only an actual player can
            // register a team-owned safe zone in this first implementation.
            return;
        }
        SafeZoneSavedData data = SafeZoneSavedData.of(server);
        String owner = SafeZoneSavedData.ownerKey(player);
        if (!data.canClaim(server, pos, owner)) {
            player.displayClientMessage(Component.literal(
                    "Hideout Core: 他チームのセーフゾーンと重なるため設置できません"), false);
            server.removeBlock(pos, false);
            return;
        }
        Zone old = data.activeCore(player);
        data.claim(server, pos, owner);
        player.displayClientMessage(Component.literal(
                "Hideout Core起動：保護半径" + HideoutConfig.horizontal() +
                "マス（上下" + HideoutConfig.vertical() + "マス）" +
                (old == null ? "" : "。旧拠点の保護は解除されました")), false);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock() &&
                level instanceof ServerLevel server) {
            SafeZoneSavedData.of(server).unregister(server, pos);
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
