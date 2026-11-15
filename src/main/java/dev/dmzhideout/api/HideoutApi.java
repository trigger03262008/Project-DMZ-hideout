package dev.dmzhideout.api;

import dev.dmzhideout.zone.SafeZoneSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small server-side extension point for future Health regeneration, P.D.A BASE
 * overlays and party plugins. Never expose mutable SavedData to consumers.
 */
public final class HideoutApi {
    public record Home(String ownerKey, String dimension, BlockPos corePosition) { }
    private HideoutApi() { }

    public static boolean isSafeZone(ServerLevel level, BlockPos pos) {
        return SafeZoneSavedData.of(level).at(level, pos) != null;
    }

    public static boolean isSafeZone(ServerPlayer player) {
        return isSafeZone(player.serverLevel(), player.blockPosition());
    }

    public static boolean canModify(ServerPlayer player, BlockPos pos) {
        SafeZoneSavedData saved = SafeZoneSavedData.of(player.serverLevel());
        var zone = saved.at(player.serverLevel(), pos);
        return zone == null || saved.isOwner(player, zone);
    }

    public static Home homeOf(ServerPlayer player) {
        var zone = SafeZoneSavedData.of(player.serverLevel()).activeCore(player);
        return zone == null ? null :
                new Home(zone.ownerKey(), zone.dimension(), zone.center());
    }
}
