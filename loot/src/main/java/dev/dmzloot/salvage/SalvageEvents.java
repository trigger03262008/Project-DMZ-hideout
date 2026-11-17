package dev.dmzloot.salvage;

import dev.dmzloot.DMZLoot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shift + right click a held junk item in the field to salvage. */
@Mod.EventBusSubscriber(modid=DMZLoot.ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class SalvageEvents {
    private SalvageEvents(){}
    @SubscribeEvent
    public static void portable(PlayerInteractEvent.RightClickItem event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!player.isShiftKeyDown()||!LootOnlyMainHand(event))return;
        if(SalvageRecipes.forStack(player.getMainHandItem())==null)return;
        SalvageService.salvage(player,null);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    private static boolean LootOnlyMainHand(PlayerInteractEvent.RightClickItem event){
        return event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND;
    }
}
