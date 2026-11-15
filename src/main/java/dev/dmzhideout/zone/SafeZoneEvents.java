package dev.dmzhideout.zone;

import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.config.HideoutConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Early, server-side protection guards. For automation and mob griefing the
 * first slice is conservative: non-player actors cannot edit protected terrain.
 * Full integration with team-aware machines is a future explicit extension.
 */
@Mod.EventBusSubscriber(modid = DMZHideout.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafeZoneEvents {
    private SafeZoneEvents() { }

    private static boolean isSafe(ServerLevel world, BlockPos pos) {
        return SafeZoneSavedData.of(world).at(world, pos) != null;
    }

    private static boolean denied(ServerLevel level, BlockPos pos, Entity actor) {
        SafeZoneSavedData data = SafeZoneSavedData.of(level);
        SafeZoneSavedData.Zone zone = data.at(level, pos);
        return zone != null && (!(actor instanceof ServerPlayer player) ||
                !data.isOwner(player, zone));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level &&
                denied(level, event.getPos(), event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placeBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level &&
                denied(level, event.getPos(), event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void changeFluid(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isSafe(level, event.getPos())) {
            // Forge's fluid hook consumes the replacement BlockState; canceling
            // alone is not reliable across 1.20.x Forge versions.
            event.setNewState(event.getOriginalState());
            if (event.isCancelable()) event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide ||
                !(event.getEntity() instanceof ServerPlayer player) ||
                !(event.getLevel() instanceof ServerLevel level)) return;
        if (HideoutConfig.PREVENT_FOREIGN_INTERACTION.get() &&
                denied(level, event.getPos(), player)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide ||
                !(event.getEntity() instanceof ServerPlayer player) ||
                !(event.getLevel() instanceof ServerLevel level)) return;
        if (denied(level, event.getPos(), player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide ||
                !(event.getEntity() instanceof ServerPlayer player) ||
                !(event.getLevel() instanceof ServerLevel level)) return;
        if (HideoutConfig.PREVENT_FOREIGN_INTERACTION.get() &&
                denied(level, event.getTarget().blockPosition(), player)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private static boolean denyDamage(net.minecraft.world.damagesource.DamageSource source,
                                      Entity target) {
        if (target instanceof ServerPlayer victim &&
                victim.level() instanceof ServerLevel level &&
                HideoutConfig.PROTECT_FROM_DAMAGE.get() &&
                isSafe(level, victim.blockPosition())) return true;
        // Prevent invulnerable sniping *from* the safe zone.
        Entity attacker = source.getEntity();
        return attacker instanceof ServerPlayer player &&
                player.level() instanceof ServerLevel level &&
                isSafe(level, player.blockPosition());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (denyDamage(event.getSource(), event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (denyDamage(event.getSource(), event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) {
            // Conservative boundary guard if a movement helper is not provided.
            if (isSafe(level, event.getPos()) || isSafe(level, event.getFaceOffsetPos()))
                event.setCanceled(true);
            return;
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            if (isSafe(level, pos)) { event.setCanceled(true); return; }
        }
        for (BlockPos pos : resolver.getToPush()) {
            // Cannot move an outsider's block into, or an owner's block out of,
            // the claim. Piston automation is intentionally untrusted for now.
            if (isSafe(level, pos) || isSafe(level, pos.relative(resolver.getPushDirection()))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // A blast in / near a base must not remove its protected blocks.
        event.getAffectedBlocks().removeIf(pos -> isSafe(level, pos));
        if (HideoutConfig.PROTECT_FROM_DAMAGE.get())
            event.getAffectedEntities().removeIf(entity ->
                    entity instanceof ServerPlayer player &&
                    isSafe(level, player.blockPosition()));
    }
}
