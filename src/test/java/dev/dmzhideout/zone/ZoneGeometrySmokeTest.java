package dev.dmzhideout.zone;

/** Run with plain javac/java, no Forge environment required. */
public final class ZoneGeometrySmokeTest {
    public static void main(String[] args) {
        assert ZoneGeometry.contains(0, 64, 0, 0, 64, 0, 12, 12);
        assert ZoneGeometry.contains(0, 64, 0, 12, 64, 0, 12, 12);
        assert !ZoneGeometry.contains(0, 64, 0, 13, 64, 0, 12, 12);
        assert ZoneGeometry.contains(0, 64, 0, 0, 76, 0, 12, 12);
        assert !ZoneGeometry.contains(0, 64, 0, 0, 77, 0, 12, 12);
        assert !ZoneGeometry.contains(0, 64, 0, 12, 64, 12, 12, 12);
        assert ZoneGeometry.overlaps(0, 64, 0, 24, 64, 0, 12, 12);
        assert !ZoneGeometry.overlaps(0, 64, 0, 25, 64, 0, 12, 12);
        assert !ZoneGeometry.overlaps(0, 64, 0, 0, 90, 0, 12, 12);
        assert !ZoneGeometry.overlaps(0, 64, 0, 0, 64, 25, 12, 12);
        // Large coordinates: squared-distance arithmetic must not overflow int.
        assert !ZoneGeometry.contains(-30000000, 64, -30000000, 30000000, 64, 30000000, 12, 12);
        System.out.println("Safe-zone geometry tests: OK");
    }
}
