package dev.dmzhideout.zone;

import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.config.HideoutConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * v0.1.1 visual boundary only. Does not touch damage, permissions or SavedData.
 *
 * The server selects nearby viewers, avoiding any separate client network
 * protocol. Each viewer sees a circular slice at their current Y and cardinal
 * vertical markers; hence even underground/raised bases remain inspectable.
 * The ring follows the exact configured radius and vertical bounds.
 */
@Mod.EventBusSubscriber(modid = DMZHideout.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafeZoneParticleEvents {
    private static final DustParticleOptions FRIENDLY = new DustParticleOptions(
            new Vector3f(0.20F, 0.88F, 0.72F), 0.90F);
    private static final DustParticleOptions FOREIGN = new DustParticleOptions(
            new Vector3f(0.96F, 0.51F, 0.21F), 0.90F);
    private static final double TWO_PI = 2.0D * Math.PI;

    private SafeZoneParticleEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END ||
                !(event.player instanceof ServerPlayer player) ||
                !HideoutConfig.SHOW_BOUNDARY_PARTICLES.get()) return;

        int interval = HideoutConfig.PARTICLE_INTERVAL_TICKS.get();
        if (player.tickCount % interval != 0) return;

        ServerLevel level = player.serverLevel();
        int radius = HideoutConfig.horizontal();
        int vertical = HideoutConfig.vertical();
        int margin = HideoutConfig.PARTICLE_VIEW_MARGIN.get();
        int pointCount = HideoutConfig.PARTICLE_RING_POINTS.get();
        SafeZoneSavedData data = SafeZoneSavedData.of(level);

        for (SafeZoneSavedData.Zone zone : data.activeZones()) {
            if (!zone.dimension().equals(level.dimension().location().toString()))
                continue;
            BlockPos pos = zone.center();
            if (!SafeZoneParticleMath.closeEnough(
                    pos.getX(), pos.getY(), pos.getZ(),
                    player.getX(), player.getY(), player.getZ(),
                    radius, vertical, margin)) continue;

            DustParticleOptions dust = data.isOwner(player, zone) ? FRIENDLY : FOREIGN;
            // The moving middle ring is at the observer's eye/chest elevation,
            // clamped to the actual top/bottom of the safe-zone volume.
            double y = SafeZoneParticleMath.sliceHeight(
                    pos.getY(), vertical, player.getEyeY() - 0.25D);
            for (int i = 0; i < pointCount; i++) {
                double angle = TWO_PI * i / pointCount;
                emit(level, player, dust,
                        SafeZoneParticleMath.ringX(pos.getX(), radius, angle),
                        y,
                        SafeZoneParticleMath.ringZ(pos.getZ(), radius, angle));
            }

            // Four slim vertical guideposts reveal the otherwise invisible
            // cylinder height. Bottom/top bounds are protected block centres.
            for (int side = 0; side < 4; side++) {
                double angle = Math.PI * side / 2.0D;
                double x = SafeZoneParticleMath.ringX(pos.getX(), radius, angle);
                double z = SafeZoneParticleMath.ringZ(pos.getZ(), radius, angle);
                for (int step = 0; step <= 4; step++) {
                    double markerY = pos.getY() - vertical + 0.5D
                            + (2D * vertical * step / 4D);
                    // Do not emit duplicates at the observer's current slice.
                    if (Math.abs(markerY - y) > 0.3D)
                        emit(level, player, dust, x, markerY, z);
                }
            }
        }
    }

    private static void emit(ServerLevel level, ServerPlayer target,
                             DustParticleOptions dust, double x, double y, double z) {
        // Vanilla's 32-block non-forced range naturally skips distant boundary
        // points. Avoid transmitting particles that cannot be rendered anyway.
        double dx = target.getX() - x;
        double dy = target.getEyeY() - y;
        double dz = target.getZ() - z;
        if (dx * dx + dy * dy + dz * dz > 32.0D * 32.0D) return;
        level.sendParticles(target, dust, false, x, y, z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
