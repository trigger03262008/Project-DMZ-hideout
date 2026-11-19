package dev.dmzhideout.zone;

/** Pure geometric helpers for rendering the safe-zone boundary. */
public final class SafeZoneParticleMath {
    private SafeZoneParticleMath() { }

    /** Cached unit circle shared by all viewers of the configured ring size. */
    private record Circle(int count, double[] cos, double[] sin) { }
    private static volatile Circle cachedCircle;

    private static Circle circle(int count) {
        if (count < 1 || count > 1024)
            throw new IllegalArgumentException("invalid ring point count");
        Circle snapshot = cachedCircle;
        if (snapshot != null && snapshot.count() == count) return snapshot;
        synchronized (SafeZoneParticleMath.class) {
            snapshot = cachedCircle;
            if (snapshot == null || snapshot.count() != count) {
                double[] xs = new double[count];
                double[] zs = new double[count];
                for (int i = 0; i < count; i++) {
                    double angle = 2.0D * Math.PI * i / count;
                    xs[i] = Math.cos(angle);
                    zs[i] = Math.sin(angle);
                }
                cachedCircle = snapshot = new Circle(count, xs, zs);
            }
        }
        return snapshot;
    }

    /** Uses cached trigonometry; mathematically identical to ringX(angle). */
    public static double ringX(int cx, int radius, int index, int count) {
        return cx + 0.5D + boundaryRadius(radius) * circle(count).cos()[index];
    }

    /** Uses cached trigonometry; mathematically identical to ringZ(angle). */
    public static double ringZ(int cz, int radius, int index, int count) {
        return cz + 0.5D + boundaryRadius(radius) * circle(count).sin()[index];
    }

    /** Whether the player is close enough to benefit from a boundary outline. */
    public static boolean closeEnough(
            int cx, int cy, int cz, double px, double py, double pz,
            int radius, int vertical, int margin) {
        double dx = px - (cx + 0.5D);
        double dz = pz - (cz + 0.5D);
        double max = radius + margin + 0.5D;
        return Math.abs(py - (cy + 0.5D)) <= vertical + margin + 2D
                && dx * dx + dz * dz <= max * max;
    }

    /** The horizontal boundary slice follows the observer, but stays in the zone. */
    public static double sliceHeight(int centerY, int vertical, double observerY) {
        return Math.max(centerY - vertical + 0.5D,
                Math.min(centerY + vertical + 0.5D, observerY));
    }

    /** Half a block outside the last protected block's centre. */
    public static double boundaryRadius(int radius) {
        return radius + 0.5D;
    }

    public static double ringX(int cx, int radius, double angle) {
        return cx + 0.5D + boundaryRadius(radius) * Math.cos(angle);
    }

    public static double ringZ(int cz, int radius, double angle) {
        return cz + 0.5D + boundaryRadius(radius) * Math.sin(angle);
    }
}
