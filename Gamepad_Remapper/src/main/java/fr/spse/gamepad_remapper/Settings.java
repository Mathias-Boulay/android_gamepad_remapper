package fr.spse.gamepad_remapper;

import static android.view.MotionEvent.AXIS_BRAKE;
import static android.view.MotionEvent.AXIS_GAS;
import static android.view.MotionEvent.AXIS_HAT_X;
import static android.view.MotionEvent.AXIS_HAT_Y;
import static android.view.MotionEvent.AXIS_LTRIGGER;
import static android.view.MotionEvent.AXIS_RTRIGGER;
import static android.view.MotionEvent.AXIS_RX;
import static android.view.MotionEvent.AXIS_RY;
import static android.view.MotionEvent.AXIS_RZ;
import static android.view.MotionEvent.AXIS_THROTTLE;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;
import static android.view.MotionEvent.AXIS_Z;

public class Settings {
    /* Yes it is meant to be package private */
    public final static int[] SUPPORTED_AXIS = new int[]{AXIS_HAT_X, AXIS_HAT_Y, AXIS_RX, AXIS_RY, AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_GAS, AXIS_BRAKE, AXIS_THROTTLE, AXIS_RTRIGGER, AXIS_LTRIGGER};

    /**
     * Deadzone at 100% if the device does not declare his properly
     */
    public static final float DEADZONE_MIN = 0.1f;
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
