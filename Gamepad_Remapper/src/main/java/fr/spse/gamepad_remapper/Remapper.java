package fr.spse.gamepad_remapper;

import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;
import static android.view.MotionEvent.AXIS_BRAKE;
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

import static fr.spse.gamepad_remapper.Settings.SUPPORTED_AXIS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

/**
 * Class able to map inputs from one way or another
 */
public class Remapper {

    public static final String SHARED_PREFERENCE_KEY = "remapper_preference";
    public static final int AXIS_NONE = -1;

    public static final int DPAD_CENTER = -9;
    private static final int DPAD_UP = -10;
    private static final int DPAD_RIGHT = -11;
    private static final int DPAD_DOWN = -12;
    private static final int DPAD_LEFT = -13;

    private static final float AXIS_TO_KEY_ACTIVATION_THRESHOLD = 0.6f;
    private static final float AXIS_TO_KEY_RESET_THRESHOLD = 0.4f;

    private final Map<Integer, Integer> keyMap, motionMap;
    private final Map<Integer, Integer> reverseKeyMap = new ArrayMap<>();
    private final Map<Integer, Integer> reverseMotionMap = new ArrayMap<>();

    /* Store current buttons value */
    private final SparseArray<Float> currentKeyValues = new SparseArray<>();
    private final SparseArray<Float> currentMotionValues = new SparseArray<>();

    public Remapper(Map<Integer, Integer> keyMap, Map<Integer, Integer> motionMap) {
        this.keyMap = keyMap;
        this.motionMap = motionMap;

        for (Map.Entry<Integer, Integer> entry : keyMap.entrySet()) {
            reverseKeyMap.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<Integer, Integer> entry : motionMap.entrySet()) {
            reverseMotionMap.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Load the default Remapper data from the shared preferences
     *
     * @param context A context object, necessary to fetch SharedPreferences
     */
    public Remapper(Context context) throws JSONException {
        this(context, "default_map");
    }

    /**
     * Load the Remapper data from the shared preferences
     *
     * @param context A context object, necessary to fetch SharedPreferences
     * @param name    The name of the map stored
     */
    public Remapper(Context context, String name) throws JSONException {
        keyMap = new ArrayMap<>();
        motionMap = new ArrayMap<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);

        JSONObject fusedMaps = new JSONObject(sharedPreferences.getString(name, ""));
        JSONObject keyMap = fusedMaps.getJSONObject("keyMap");
        JSONObject motionMap = fusedMaps.getJSONObject("motionMap");

        Iterator<String> keysItr = keyMap.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            int value = keyMap.getInt(key);
            this.keyMap.put(Integer.valueOf(key), value);
        }

        keysItr = motionMap.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            int value = motionMap.getInt(key);
            this.motionMap.put(Integer.valueOf(key), value);
        }

        for (Map.Entry<Integer, Integer> entry : this.keyMap.entrySet()) {
            reverseKeyMap.put(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<Integer, Integer> entry : this.motionMap.entrySet()) {
            reverseMotionMap.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Changes the dpad value by another value with the same meaning.
     * It is done because some axis share the same value as KEYCODE_DPAD_XX
     *
     * @param keycode The keycode to transform
     */
    static int transformKeyEventInput(int keycode) {
        if (keycode == KEYCODE_DPAD_UP || keycode == KEYCODE_DPAD_DOWN) return AXIS_HAT_Y;
        if (keycode == KEYCODE_DPAD_RIGHT || keycode == KEYCODE_DPAD_LEFT) return AXIS_HAT_X;
        return keycode;
    }

    /**
     * Removes all current preferences from the data
     */
    public static void wipePreferences(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences(SHARED_PREFERENCE_KEY);
        } else {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
            for (String key : sharedPreferences.getAll().keySet()) {
                sharedPreferences.edit().remove(key).apply();
            }
        }
    }

    private static float getDeadzone(MotionEvent event, int axis) {
        try {
            InputDevice.MotionRange range = event.getDevice().getMotionRange(axis, InputDevice.SOURCE_JOYSTICK);
            float deadzone = 0;
            if (range != null) {
                deadzone = range.getFlat() * Settings.getDeadzoneScale();
            }
            return Math.max(deadzone, Settings.DEADZONE_MIN * Settings.getDeadzoneScale());
        } catch (Exception e) {
            Log.e(Remapper.class.toString(), "Dynamic Deadzone is not supported ");
            return 0.2f;
        }
    }

    private static double getMagnitude(float x, float y) {
        return RemapperUtils.dist(0, 0, Math.abs(x), Math.abs(y));
    }

    /**
     * Saves the Remapper data inside its own shared preference file
     *
     * @param context A context object, necessary to fetch SharedPreferences
     */
    public void save(Context context) {
        save(context, "default_map");
    }


    /**
     * Saves the Remapper data inside its own shared preference file
     *
     * @param context A context object, necessary to fetch SharedPreferences
     * @param name    The name for the file.
     */
    public void save(Context context, String name) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        JSONObject keyMap = new JSONObject();
        for (Map.Entry<Integer, Integer> entry : this.keyMap.entrySet()) {
            try {
                keyMap.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (JSONException e) {
                Log.e(Remapper.class.toString(), "Failed to save to shared preferences");
            }
        }

        JSONObject motionMap = new JSONObject();
        for (Map.Entry<Integer, Integer> entry : this.motionMap.entrySet()) {
            try {
                motionMap.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (JSONException e) {
                Log.e(Remapper.class.toString(), "Failed to save to shared preferences");
            }
        }

        JSONObject fusedMaps = new JSONObject();
        try {
            fusedMaps.put("keyMap", keyMap);
            fusedMaps.put("motionMap", motionMap);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        preferences.edit().putString(name, fusedMaps.toString()).apply();
    }

    /**
     * If the event is a valid Gamepad event, call the GamepadHandler method.
     * Note that the handler won't be called if there is no value change.
     *
     * @param event   The current MotionEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleMotionEventInput(MotionEvent event, GamepadHandler handler) {
        if (!RemapperView.isGamepadMotionEvent(event)) return false;

        handleMotionIfDifferent(AXIS_HAT_X, getRemappedValue(AXIS_HAT_X, event), handler);
        handleMotionIfDifferent(AXIS_HAT_Y, getRemappedValue(AXIS_HAT_Y, event), handler);
        handleMotionIfDifferent(AXIS_RTRIGGER, getRemappedValue(AXIS_RTRIGGER, event), handler);
        handleMotionIfDifferent(AXIS_LTRIGGER, getRemappedValue(AXIS_LTRIGGER, event), handler);

        handleJoystickInput(event, handler, AXIS_X, AXIS_Y);
        handleJoystickInput(event, handler, AXIS_Z, AXIS_RZ);
        return true;
    }

    /**
     * Same as the handleMotionEvent but applies a deadzone
     */
    private void handleJoystickInput(MotionEvent event, GamepadHandler handler, int horizontalAxis, int verticalAxis) {
        float x = getRemappedValue(horizontalAxis, event);
        float y = getRemappedValue(verticalAxis, event);

        double magnitude = getMagnitude(x, y);
        float deadzone = getDeadzone(event, getRemappedSource(event, horizontalAxis)); // FIXME should we query both axis ?
        if (magnitude < deadzone) {
            x = 0;
            y = 0;
        } else {
            // compensate the value for deadzone
            x = (float) ((x / magnitude) * ((magnitude - deadzone) / (1 - deadzone)));
            y = (float) ((y / magnitude) * ((magnitude - deadzone) / (1 - deadzone)));
        }
        
        handleMotionIfDifferent(horizontalAxis, x, handler);
        handleMotionIfDifferent(verticalAxis, y, handler);
    }

    private void handleMotionIfDifferent(int mappedSource, float value, GamepadHandler handler) {
        Float lastValue = currentMotionValues.get(mappedSource);
        if (lastValue == null || lastValue != value) {
            handler.handleGamepadInput(mappedSource, value);
            currentMotionValues.put(mappedSource, value);
        }
    }

    /**
     * If the event is a valid Gamepad event, call the GamepadHandler method
     *
     * @param event   The current KeyEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleKeyEventInput(KeyEvent event, GamepadHandler handler) {
        if (!RemapperView.isGamepadKeyEvent(event)) return false;
        if (event.getKeyCode() == KEYCODE_UNKNOWN) return false;
        if (event.getRepeatCount() > 0) return false;

        int mappedSource = getRemappedSource(event);
        float currentValue = getRemappedValue(mappedSource, event);
        Float lastValue = currentKeyValues.get(mappedSource);
        if (lastValue == null || currentValue != lastValue) {
            handler.handleGamepadInput(mappedSource, currentValue);
            currentKeyValues.put(mappedSource, currentValue);
        }
        return true;
    }

    /**
     * If remapped, get the mapped source from keyEvent
     */
    private int getRemappedSource(KeyEvent event) {
        int translatedSource = transformKeyEventInput(event.getKeyCode());
        Integer mappedValue = keyMap.get(translatedSource);
        return mappedValue == null ? translatedSource : mappedValue;
    }

    /**
     * If remapped, get the mapped source from MotionEvent
     */
    private int getRemappedSource(MotionEvent event, int axisSource) {
        Integer mappedValue = reverseMotionMap.get(axisSource);
        return mappedValue == null ? axisSource : mappedValue;
    }

    /**
     * Get the converted value for the given mapped source
     */
    private static float getRemappedValue(int mappedSource, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
            // Special case for DPADs, there are never remapped to anything else. So we consider them properly mapped.
            if ((mappedSource == AXIS_HAT_Y && keyEvent.getKeyCode() == KEYCODE_DPAD_UP)
                    || (mappedSource == AXIS_HAT_X && keyEvent.getKeyCode() == KEYCODE_DPAD_LEFT)
            ) {
                return -1f;
            }
            return 1f;
        }
        return 0f;
    }

    /**
     * Get the converted value for the given mapped source
     */
    private float getRemappedValue(int orignalSource, MotionEvent motionEvent) {
        int mappedSource = getRemappedSource(motionEvent, orignalSource);

        if (isAxis(mappedSource)) {
            return motionEvent.getAxisValue(mappedSource);
        }

        // Else, convert to a keyEvent action
        // Assume that only one button is mapped to the final value
        // Since the even is converted back into a "keyevent", the values are 0 or 1
        boolean isEnabled = currentMotionValues.get(orignalSource, 0.0f) == 1.0f;
        float absoluteValue = Math.abs(motionEvent.getAxisValue(mappedSource));
        if (isEnabled) {
            return  absoluteValue >= AXIS_TO_KEY_RESET_THRESHOLD ? 1 : 0;
        } else {
            return absoluteValue >= AXIS_TO_KEY_ACTIVATION_THRESHOLD ? 1 : 0;
        }
    }

    /**
     * @return Whether the input source is a **gamepad** axis.
     */
    private boolean isAxis(int inputSource) {
        for (int axis : SUPPORTED_AXIS) {
            if (axis == inputSource) return true;
        }
        return false;
    }
}

