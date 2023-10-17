package fr.spse.gamepad_remapper;

public class Settings {
    /**
     * Deadzone at 100%
     */
    public static float DEADZONE_MIN = 0.1f;

    private static float DEADZONE_SCALE = 1f;

    private Settings() {
    }

    public static float getDeadzoneScale() {
        return DEADZONE_SCALE;
    }

    /**
     * Set the deadzone scale, relative to what's declared by the joystick.
     *
     * @param scale 1F is a 100% of the original deadzone
     */
    public static void setDeadzoneScale(float scale) {
        DEADZONE_SCALE = scale;
    }
}
