package dev.dmzhideout.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** A core's zone is a horizontal cylinder with finite vertical extent. */
public final class HideoutConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HORIZONTAL_RADIUS;
    public static final ForgeConfigSpec.IntValue VERTICAL_RADIUS;
    public static final ForgeConfigSpec.BooleanValue PREVENT_FOREIGN_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue PROTECT_FROM_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_BOUNDARY_PARTICLES;
    public static final ForgeConfigSpec.IntValue PARTICLE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue PARTICLE_RING_POINTS;
    public static final ForgeConfigSpec.IntValue PARTICLE_VIEW_MARGIN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("safeZone");
        HORIZONTAL_RADIUS = b.comment("Blocks outward from Hideout Core in X/Z (circle)")
                .defineInRange("horizontalRadius", 12, 3, 64);
        VERTICAL_RADIUS = b.comment("Blocks above and below Hideout Core")
                .defineInRange("verticalRadius", 12, 3, 64);
        PREVENT_FOREIGN_INTERACTION = b.comment("Deny containers, doors and machines to outsiders")
                .define("protectInteractions", true);
        PROTECT_FROM_DAMAGE = b.comment("Players in the core zone are immune to incoming damage")
                .define("invulnerablePlayers", true);
        b.pop();

        b.push("visualization");
        SHOW_BOUNDARY_PARTICLES = b.comment(
                "Show softly glowing boundary particles around ACTIVE Hideout Core zones.",
                "Purely visual. The protection radius is unaffected.")
                .define("showBoundaryParticles", true);
        PARTICLE_INTERVAL_TICKS = b.comment(
                "Ticks between particle outlines (20 ticks = about one second).",
                "Larger values reduce network and rendering cost.")
                .defineInRange("particleIntervalTicks", 20, 5, 100);
        PARTICLE_RING_POINTS = b.comment(
                "Points around the circular boundary. Higher values produce a smoother ring.",
                "Particles are only sent to nearby players.")
                .defineInRange("particleRingPoints", 40, 12, 128);
        PARTICLE_VIEW_MARGIN = b.comment(
                "Display the zone boundary to a player up to this many blocks outside its radius.",
                "This does not grant protection outside the actual safe zone.")
                .defineInRange("particleViewMargin", 16, 0, 64);
        b.pop();
        SPEC = b.build();
    }

    private HideoutConfig() { }

    public static int horizontal() { return HORIZONTAL_RADIUS.get(); }
    public static int vertical() { return VERTICAL_RADIUS.get(); }
}
