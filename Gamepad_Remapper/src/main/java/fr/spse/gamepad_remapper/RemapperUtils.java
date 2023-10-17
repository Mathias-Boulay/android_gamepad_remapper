package fr.spse.gamepad_remapper;

public class RemapperUtils {
    private RemapperUtils() {
    }

    /**
     * Returns the distance between two points.
     */
    public static float dist(float x1, float y1, float x2, float y2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        return (float) Math.hypot(x, y);
    }
}
