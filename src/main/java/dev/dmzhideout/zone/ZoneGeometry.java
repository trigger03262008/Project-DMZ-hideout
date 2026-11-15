package dev.dmzhideout.zone;

/** Pure functions also exercised by dependency-free smoke tests. */
public final class ZoneGeometry {
    private ZoneGeometry() { }

    public static boolean contains(int cx, int cy, int cz, int x, int y, int z,
                                   int horizontalRadius, int verticalRadius) {
        long dx = (long) x - cx;
        long dz = (long) z - cz;
        long dy = (long) y - cy;
        return Math.abs(dy) <= verticalRadius && dx * dx + dz * dz <=
                (long) horizontalRadius * horizontalRadius;
    }

    public static boolean overlaps(int ax, int ay, int az, int bx, int by, int bz,
                                   int horizontalRadius, int verticalRadius) {
        long dx = (long) ax - bx;
        long dz = (long) az - bz;
        long dy = (long) ay - by;
        return Math.abs(dy) <= 2L * verticalRadius && dx * dx + dz * dz <=
                4L * horizontalRadius * horizontalRadius;
    }
}
