package dev.dmzhideout.block;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.ModList;

/**
 * Hideout-owned workbench. Loot is OPTIONAL: no hard JVM reference to dmzloot.
 * The Loot API alone handles all disassembly, repair, inventory validation.
 */
public final class SalvageWorkbenchBlock extends Block {
    public SalvageWorkbenchBlock(BlockBehaviour.Properties properties){
        super(properties);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,
                                          Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer server))return InteractionResult.PASS;
        if(!ModList.get().isLoaded("dmzloot")){
            server.displayClientMessage(Component.literal(
                    "このワークベンチには Project DMZ: Loot が必要です"),true);
            return InteractionResult.CONSUME;
        }
        try {
            Class<?> api=Class.forName("dev.dmzloot.api.LootApi");
            Method use=api.getMethod("useWorkbench",ServerPlayer.class,BlockPos.class);
            if(!Boolean.TRUE.equals(use.invoke(null,server,pos)))
                server.displayClientMessage(Component.literal(
                        "ワークベンチ処理に失敗しました"),true);
        }catch(ReflectiveOperationException|LinkageError error){
            server.displayClientMessage(Component.literal(
                    "Lootとの連携に失敗しました。サーバーログを確認してください"),true);
            com.mojang.logging.LogUtils.getLogger().error(
                    "Hideout salvage workbench could not call LootApi",error);
        }
        return InteractionResult.CONSUME;
    }
}
