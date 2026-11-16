package dev.dmzhideout.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Shared block class for the 3 workbench-style facilities. */
public final class FacilityBlock extends BaseEntityBlock {
    public enum Kind { STASH, MED, TRADE }
    private final Kind kind;

    public FacilityBlock(Kind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case STASH -> new StashBlockEntity(pos, state);
            case MED, TRADE -> new FacilityBlockEntity(pos, state, kind);
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof StashBlockEntity stash)
            stash.establishOwner(player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof StashBlockEntity stash) {
            if (!stash.canAccess(sp)) {
                sp.displayClientMessage(Component.literal("Stash: 権限がありません"), true);
                return InteractionResult.CONSUME;
            }
            NetworkHooks.openScreen(sp, stash, pos);
        } else if (entity instanceof FacilityBlockEntity facility) {
            NetworkHooks.openScreen(sp, facility, pos);
        }
        return InteractionResult.CONSUME;
    }
}
