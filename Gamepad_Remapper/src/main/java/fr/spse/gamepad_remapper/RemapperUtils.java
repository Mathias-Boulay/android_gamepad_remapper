package fr.spse.gamepad_remapper;

import android.content.res.Resources;

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

    public static String buttonLabel(Resources resources, int buttonId) {
        return resources.getString(R.string.bind_process_waiting_button, resources.getString(buttonId));
    }

    private static String buttonLabel(Resources resources, int buttonId, int directionId) {
        return resources.getString(R.string.bind_process_waiting_button,
                resources.getString(directionId) + " " +
                        resources.getString(buttonId)
        );
    }

    public static String joystickLabel(Resources resources, int positionId, int directionId) {
        return resources.getString(R.string.bind_process_waiting_joystick,
                resources.getString(positionId),
                resources.getString(directionId)
        );
    }

    public static String joystickButtonLabel(Resources resources, int positionId) {
        return resources.getString(R.string.bind_process_waiting_button,
                resources.getString(positionId) + " " +
                        resources.getString(R.string.button_joystick)
        );
    }

    public static String dpadLabel(Resources resources, int directionId) {
        return buttonLabel(resources, R.string.button_dpad, directionId);
    }

    public static String shoulderLabel(Resources resources, int directionId) {
        return buttonLabel(resources, R.string.button_shoulder, directionId);
    }

    public static String triggerLabel(Resources resources, int directionId) {
        return buttonLabel(resources, R.string.button_trigger, directionId);
    }
}
