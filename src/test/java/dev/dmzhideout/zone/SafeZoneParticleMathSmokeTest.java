package dev.dmzhideout.zone;

/** Regression checks for v0.1.1's moving boundary and player distance cull. */
public final class SafeZoneParticleMathSmokeTest {
    public static void main(String[] args) {
        assert SafeZoneParticleMath.closeEnough(0, 64, 0,
                0.5D, 64.5D, 0.5D, 12, 12, 16);
        assert SafeZoneParticleMath.closeEnough(0, 64, 0,
                28.5D, 64.5D, 0.5D, 12, 12, 16);
        assert !SafeZoneParticleMath.closeEnough(0, 64, 0,
                30D, 64.5D, 0.5D, 12, 12, 16);
        assert !SafeZoneParticleMath.closeEnough(0, 64, 0,
                0.5D, 100D, 0.5D, 12, 12, 16);

        assert SafeZoneParticleMath.sliceHeight(64, 12, 100) == 76.5D;
        assert SafeZoneParticleMath.sliceHeight(64, 12, -100) == 52.5D;
        assert SafeZoneParticleMath.sliceHeight(64, 12, 66) == 66D;
        assert SafeZoneParticleMath.boundaryRadius(12) == 12.5D;
        assert Math.abs(SafeZoneParticleMath.ringX(0, 12, 0) - 13.0D) < 1.0E-10D;
        assert Math.abs(SafeZoneParticleMath.ringZ(0, 12, Math.PI / 2) - 13.0D)
                < 1.0E-10D;

        // The protection rules are still based on the ORIGINAL zone geometry.
        assert ZoneGeometry.contains(0, 64, 0, 12, 64, 0, 12, 12);
        assert !ZoneGeometry.contains(0, 64, 0, 13, 64, 0, 12, 12);

        // Caching must preserve all coordinates across configurable ring sizes.
        for (int points : new int[]{12, 40, 128}) {
            for (int i = 0; i < points; i++) {
                double angle = 2.0D * Math.PI * i / points;
                assert Math.abs(SafeZoneParticleMath.ringX(-12, 12, angle)
                        - SafeZoneParticleMath.ringX(-12, 12, i, points)) < 1.0E-10D;
                assert Math.abs(SafeZoneParticleMath.ringZ(8, 12, angle)
                        - SafeZoneParticleMath.ringZ(8, 12, i, points)) < 1.0E-10D;
            }
        }
        // A change of configured point count must rebuild the cache safely.
        assert Math.abs(SafeZoneParticleMath.ringX(0, 12, 0, 12) - 13D) < 1E-10D;
        System.out.println("Safe-zone particle outline tests: OK");
    }
}
