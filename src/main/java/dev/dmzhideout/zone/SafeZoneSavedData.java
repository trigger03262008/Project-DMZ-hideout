package dev.dmzhideout.zone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import dev.dmzhideout.config.HideoutConfig;

/**
 * One *active* core per team (or solo player) across all dimensions in a world.
 * Old physical cores are inert after relocation. The zone is not stored on a
 * movable item or player entity; owners survive logout and death.
 */
public final class SafeZoneSavedData extends SavedData {
    public record Zone(String ownerKey, String dimension, BlockPos center) { }
    private final Map<String, Zone> zones = new HashMap<>();

    private SafeZoneSavedData() { }

    public static SafeZoneSavedData of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SafeZoneSavedData::load, SafeZoneSavedData::new, "dmzhideout_safezones");
    }

    /** The scoreboard team is our temporary party provider; solo uses the UUID. */
    public static String ownerKey(ServerPlayer player) {
        return player.getTeam() == null ? "player:" + player.getUUID()
                : "team:" + player.getTeam().getName();
    }

    public static SafeZoneSavedData load(CompoundTag root) {
        SafeZoneSavedData result = new SafeZoneSavedData();
        for (Tag raw : root.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            String owner = tag.getString("Owner");
            String dimension = tag.getString("Dimension");
            if (owner.length() < 5 || owner.length() > 160 ||
                    ResourceLocation.tryParse(dimension) == null) continue;
            BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            result.zones.put(owner, new Zone(owner, dimension, pos));
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        zones.values().forEach(zone -> {
            CompoundTag z = new CompoundTag();
            z.putString("Owner", zone.ownerKey());
            z.putString("Dimension", zone.dimension());
            z.putInt("X", zone.center().getX());
            z.putInt("Y", zone.center().getY());
            z.putInt("Z", zone.center().getZ());
            list.add(z);
        });
        root.put("Zones", list);
        return root;
    }

    /** Current authoritative active zones; old relocated cores have no entry. */
    public List<Zone> activeZones() {
        return List.copyOf(zones.values());
    }

    public Zone at(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        int horizontal = HideoutConfig.horizontal();
        int vertical = HideoutConfig.vertical();
        for (Zone zone : zones.values()) {
            if (zone.dimension().equals(dimension) &&
                    ZoneGeometry.contains(zone.center().getX(), zone.center().getY(),
                            zone.center().getZ(), pos.getX(), pos.getY(), pos.getZ(),
                            horizontal, vertical)) return zone;
        }
        return null;
    }

    public boolean canClaim(ServerLevel level, BlockPos pos, String newOwner) {
        String dimension = level.dimension().location().toString();
        for (Zone zone : zones.values()) {
            if (zone.ownerKey().equals(newOwner) || !zone.dimension().equals(dimension))
                continue; // relocating an owner's old zone is legal
            if (ZoneGeometry.overlaps(zone.center().getX(), zone.center().getY(),
                    zone.center().getZ(), pos.getX(), pos.getY(), pos.getZ(),
                    HideoutConfig.horizontal(), HideoutConfig.vertical())) return false;
        }
        return true;
    }

    /** Caller must have validated no foreign-zone overlap on server thread. */
    public void claim(ServerLevel level, BlockPos pos, String owner) {
        zones.put(owner, new Zone(owner, level.dimension().location().toString(),
                pos.immutable()));
        setDirty();
    }

    /** Only unregister the currently ACTIVE core, not a deactivated old one. */
    public void unregister(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        if (zones.values().removeIf(zone -> zone.dimension().equals(dimension)
                && zone.center().equals(pos))) setDirty();
    }

    public boolean isOwner(ServerPlayer player, Zone zone) {
        return zone != null && Objects.equals(zone.ownerKey(), ownerKey(player));
    }

    public Zone activeCore(ServerPlayer player) {
        return zones.get(ownerKey(player));
    }
}
