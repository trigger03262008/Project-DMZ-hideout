package dev.dmzhideout.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** A core's zone is a horizontal cylinder with finite vertical extent. */
public final class HideoutConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HORIZONTAL_RADIUS;
    public static final ForgeConfigSpec.IntValue VERTICAL_RADIUS;
    public static final ForgeConfigSpec.BooleanValue PREVENT_FOREIGN_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue PROTECT_FROM_DAMAGE;

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
        SPEC = b.build();
    }

    private HideoutConfig() { }

    public static int horizontal() { return HORIZONTAL_RADIUS.get(); }
    public static int vertical() { return VERTICAL_RADIUS.get(); }
}
