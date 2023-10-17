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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
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

    private final Map<Integer, Integer> keyMap, motionMap;
    private final Map<Integer, Integer> reverseKeyMap = new ArrayMap<>();
    private final Map<Integer, Integer> reverseMotionMap = new ArrayMap<>();

    /* Store current buttons value */
    private SparseArray<Float> currentKeyValues = new SparseArray<>();
    private SparseArray<Float> currentMotionValues = new SparseArray<>();

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

    private static int findTriggeredAxis(MotionEvent event) {
        for (int axis : new int[]{AXIS_RX, AXIS_RY, AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_BRAKE, AXIS_THROTTLE, AXIS_RTRIGGER, AXIS_LTRIGGER}) {
            if (Math.abs(event.getAxisValue(axis)) >= 0.5) {
                return axis;
            }
        }
        return AXIS_NONE;
    }

    /**
     * Changes the dpad value by another value with the same meaning.
     * It is done because some axis share the same value as KEYCODE_DPAD_XX
     *
     * @param keycode The keycode to transform
     */
    private static int transformKeyEventInput(int keycode) {
        if (keycode == KEYCODE_DPAD_UP) return DPAD_UP;
        if (keycode == KEYCODE_DPAD_RIGHT) return DPAD_RIGHT;
        if (keycode == KEYCODE_DPAD_DOWN) return DPAD_DOWN;
        if (keycode == KEYCODE_DPAD_LEFT) return DPAD_LEFT;
        return keycode;
    }

    /**
     * Changes the dpad value by another value with the same meaning.
     * It is done because some axis share the same value as KEYCODE_DPAD_XX
     *
     * @param event The event to transform
     * @param axis  The axis to observe
     */
    private static int transformMotionEventInput(MotionEvent event, int axis) {
        if (axis == AXIS_HAT_X) {
            if (event.getAxisValue(axis) >= 0.85) return DPAD_RIGHT;
            if (event.getAxisValue(axis) <= -0.85) return DPAD_LEFT;
        }

        if (axis == AXIS_HAT_Y) {
            if (event.getAxisValue(axis) >= 0.85) return DPAD_DOWN;
            if (event.getAxisValue(axis) <= -0.85) return DPAD_UP;
        }

        return DPAD_CENTER;
    }

    /**
     * Changes the dpad value by another value with the same meaning.
     * It is done because some axis share the same value as KEYCODE_DPAD_XX
     *
     * @param event The event to transform
     * @param axis  The axis to observe
     */
    private static int transformMotionEventOutput(MotionEvent event, int axis) {
        if (axis == AXIS_HAT_X) {
            if (event.getAxisValue(axis) >= 0.5) return KEYCODE_DPAD_RIGHT;
            if (event.getAxisValue(axis) <= -0.5) return KEYCODE_DPAD_LEFT;
        }

        if (axis == AXIS_HAT_Y) {
            if (event.getAxisValue(axis) >= 0.5) return KEYCODE_DPAD_DOWN;
            if (event.getAxisValue(axis) <= -0.5) return KEYCODE_DPAD_UP;
        }

        return KEYCODE_DPAD_CENTER;
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
            return event.getDevice().getMotionRange(axis).getFlat(); // TODO handle the global deadzone setting
        } catch (Exception e) {
            Log.e(Remapper.class.toString(), "Dynamic Deadzone is not supported ");
            return 0.2f;
        }
    }

    private static double getMagnitude(float x, float y) {
        return Utils.dist(0, 0, Math.abs(x), Math.abs(y));
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

        handleMotionIfDifferent(AXIS_HAT_X, getRemappedValue(getRemappedSource(event, AXIS_HAT_X), event), handler);
        handleMotionIfDifferent(AXIS_HAT_Y, getRemappedValue(getRemappedSource(event, AXIS_HAT_Y), event), handler);
        handleMotionIfDifferent(AXIS_RTRIGGER, getRemappedValue(getRemappedSource(event, AXIS_RTRIGGER), event), handler);
        handleMotionIfDifferent(AXIS_LTRIGGER, getRemappedValue(getRemappedSource(event, AXIS_LTRIGGER), event), handler);

        handleJoystickInput(event, handler, AXIS_X, AXIS_Y);
        handleJoystickInput(event, handler, AXIS_Z, AXIS_RZ);
        return true;
    }

    /**
     * Same as the handleMotionEvent but applies a deadzone
     */
    private void handleJoystickInput(MotionEvent event, GamepadHandler handler, int horizontalAxis, int verticalAxis) {
        int originalHorizontalAxis = getRemappedSource(event, horizontalAxis);
        int originalVerticalAxis = getRemappedSource(event, verticalAxis);
        float x = getRemappedValue(originalHorizontalAxis, event);
        float y = getRemappedValue(originalVerticalAxis, event);

        double magnitude = getMagnitude(x, y);
        float deadzone = getDeadzone(event, horizontalAxis); // FIXME should we query both axis ?
        if (magnitude < deadzone) {
            x = 0;
            y = 0;
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
        Integer mappedValue = keyMap.get(transformKeyEventInput(event.getKeyCode()));
        return mappedValue == null ? event.getKeyCode() : mappedValue;
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
    private float getRemappedValue(int mappedSource, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
            return 1f;
        }
        return 0f;
    }

    /**
     * Get the converted value for the given mapped source
     */
    private float getRemappedValue(int mappedSource, MotionEvent motionEvent) {
        //Integer axis = reverseMotionMap.get(mappedSource);
        //if(axis == null) axis = mappedSource;

        if (isAxis(mappedSource)) {
            return motionEvent.getAxisValue(mappedSource);
        }

        // Else, convert to a keyEvent action
        return Math.abs(motionEvent.getAxisValue(mappedSource)) >= 0.5
                ? 1 : 0;
    }

    /**
     * @return Whether the input source is a **gamepad** axis.
     */
    private boolean isAxis(int inputSource) {
        return inputSource == AXIS_X || inputSource == AXIS_Y
                || inputSource == AXIS_Z || inputSource == AXIS_RZ
                || inputSource == AXIS_BRAKE || inputSource == AXIS_THROTTLE
                || inputSource == AXIS_LTRIGGER || inputSource == AXIS_RTRIGGER
                || inputSource == AXIS_HAT_X || inputSource == AXIS_HAT_Y;


    }
}

