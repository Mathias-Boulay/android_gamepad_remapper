package fr.spse.gamepadremapperdemo;


import static android.view.KeyEvent.KEYCODE_BUTTON_A;
import static android.view.KeyEvent.KEYCODE_BUTTON_B;
import static android.view.KeyEvent.KEYCODE_BUTTON_L1;
import static android.view.KeyEvent.KEYCODE_BUTTON_R1;
import static android.view.KeyEvent.KEYCODE_BUTTON_SELECT;
import static android.view.KeyEvent.KEYCODE_BUTTON_START;
import static android.view.KeyEvent.KEYCODE_BUTTON_THUMBL;
import static android.view.KeyEvent.KEYCODE_BUTTON_THUMBR;
import static android.view.KeyEvent.KEYCODE_BUTTON_X;
import static android.view.KeyEvent.KEYCODE_BUTTON_Y;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.MotionEvent.AXIS_HAT_X;
import static android.view.MotionEvent.AXIS_HAT_Y;
import static android.view.MotionEvent.AXIS_LTRIGGER;
import static android.view.MotionEvent.AXIS_RTRIGGER;
import static android.view.MotionEvent.AXIS_RZ;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;
import static android.view.MotionEvent.AXIS_Z;

import static fr.spse.gamepad_remapper.Remapper.SHARED_PREFERENCE_KEY;
import static fr.spse.gamepad_remapper.Settings.SUPPORTED_AXIS;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import fr.spse.gamepad_remapper.GamepadHandler;
import fr.spse.gamepad_remapper.Remapper;
import fr.spse.gamepad_remapper.RemapperManager;
import fr.spse.gamepad_remapper.RemapperView;

public class MainActivity extends Activity implements GamepadHandler {

    private final HashMap<String, Float> lastRemappedGamepadState = new HashMap<>();
    private final HashMap<String, String> lastRawGamepadState = new HashMap<>();

    private ImageView imageView;
    private RemapperManager manager;
    private TextView deadzoneTextview, lastStateTextview, lastRawStateTextView;
    private Button resetButton, exportButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image_view);
        deadzoneTextview = findViewById(R.id.deadzone_values);
        lastStateTextview = findViewById(R.id.last_state);
        lastRawStateTextView = findViewById(R.id.last_raw_state);
        resetButton = findViewById(R.id.reset_button);
        exportButton = findViewById(R.id.export_button);

        // Create a builder with all the data we need.
        // The listener here is not needed, since the builder is passed to the RemapperManager
        // which handles listening by itself.
        RemapperView.Builder builder = new RemapperView.Builder()
                .remapA(true)
                .remapB(true)
                .remapX(true)
                .remapY(true)

                .remapDpad(true)
                .remapLeftJoystick(true)
                .remapRightJoystick(true)
                .remapStart(true)
                .remapSelect(true)
                .remapLeftShoulder(true)
                .remapRightShoulder(true)
                .remapLeftTrigger(true)
                .remapRightTrigger(true);

        manager = new RemapperManager(this, builder);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = v.getContext().getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
                    builder.append(entry.getKey()).append(" : ").append(entry.getValue());
                }

                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText("error", builder));
            }
        });

        resetButton.setOnClickListener(v -> {
            Remapper.wipePreferences(v.getContext());
            manager = new RemapperManager(this, builder);
        });
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        updateRawKeyEventState(event);
        return manager.handleKeyEventInput(this, event, this) || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        InputDevice device = event.getDevice();
        if (device != null) {
            deadzoneTextview.setText(device.toString());
        }
        updateRawMotionEventState(event);
        return manager.handleMotionEventInput(this, event, this) || super.dispatchGenericMotionEvent(event);
    }

    /** stolen from the lib */
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
     * Function handling all gamepad actions
     *
     * @param code  Either a keycode (Eg. KEYBODE_BUTTON_A), either an axis (Eg. AXIS_HAT_X)
     * @param value For keycodes, 0 for released state, 1 for pressed state.
     *              For Axis, the value of the axis. Varies between 0/1 or -1/1 depending on the axis.
     */
    public void handleGamepadInput(int code, float value) {
        updateRemappedGamepadState(code, value);
        switch (code) {
            case KEYCODE_BUTTON_A:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_a, value);
                break;
            case KEYCODE_BUTTON_B:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_b, value);
                break;
            case KEYCODE_BUTTON_X:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_x, value);
                break;
            case KEYCODE_BUTTON_Y:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_y, value);
                break;

            // Shoulder buttons
            case KEYCODE_BUTTON_R1:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.shoulder_right, value);
                break;
            case KEYCODE_BUTTON_L1:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.shoulder_left, value);
                break;

            case KEYCODE_BUTTON_SELECT:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_select, value);
                break;
            case KEYCODE_BUTTON_START:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.button_start, value);
                break;

            // Joystick buttons
            case KEYCODE_BUTTON_THUMBL:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.stick_left_click, value);
                break;
            case KEYCODE_BUTTON_THUMBR:
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.stick_right_click, value);
                break;


            // DPAD
            case AXIS_HAT_X:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.dpad_right, value);
                break;
            case AXIS_HAT_Y:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.dpad_down, value);
                break;

            // Left joystick
            case AXIS_X:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.stick_left, value);
                break;
            case AXIS_Y:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.stick_left, value);
                break;

            // Right joystick
            case AXIS_Z:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.stick_right, value);
                break;
            case AXIS_RZ:
                setImageIfAbsValue(fr.spse.gamepad_remapper.R.drawable.stick_right, value);
                break;

            // Triggers
            case AXIS_RTRIGGER:
                System.out.println("AXIS_RTRIGGER - Value : " + value);
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.trigger_right, value);
                break;
            case AXIS_LTRIGGER:
                System.out.println("AXIS_LTRIGGER - Value : " + value);
                setImageIfPositiveValue(fr.spse.gamepad_remapper.R.drawable.trigger_left, value);
                break;
        }
    }

    private void updateRemappedGamepadState(int event, float state) {
        lastRemappedGamepadState.put(getKey(event), state);
        lastStateTextview.setText(buildStateToString(lastRemappedGamepadState));
    }

    public static String actionToString(int action) {
        switch (action) {
            case KeyEvent.ACTION_DOWN:
                return "DOWN";
            case KeyEvent.ACTION_UP:
                return "UP";
            case KeyEvent.ACTION_MULTIPLE:
                return "MULTIPLE";
            default:
                return Integer.toString(action);
        }
    }

    private void updateRawKeyEventState(KeyEvent event) {
        lastRawGamepadState.put(KeyEvent.keyCodeToString(event.getKeyCode()).replace("KEYCODE_", ""), actionToString(event.getAction()));
        lastRawStateTextView.setText(buildStateToString(lastRawGamepadState));
    }

    private void updateRawMotionEventState(MotionEvent event) {
        for (int axis : SUPPORTED_AXIS) {
            lastRawGamepadState.put(MotionEvent.axisToString(axis), String.valueOf(event.getAxisValue(axis)));
        }
        lastRawStateTextView.setText(buildStateToString(lastRawGamepadState));
    }


    private static String getKey(int event) {
        String key = MotionEvent.axisToString(event);
        if (key.equals(Integer.toString(event))) {
            key = KeyEvent.keyCodeToString(event);
        }
        return key;
    }

    private static CharSequence buildStateToString(Map<String, ?> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ?> test : map.entrySet()) {
            builder.append(test.getKey()).append(" : ").append(test.getValue()).append('\n');
        }
        return builder;
    }

    private void setImageIfPositiveValue(int imageId, float value) {
        if (value >= 0) {
            imageView.setImageDrawable(getResources().getDrawable(imageId, getTheme()));
        }
    }

    private void setImageIfAbsValue(int imageId, float value) {
        if (Math.abs(value) >= 0) {
            imageView.setImageDrawable(getResources().getDrawable(imageId, getTheme()));
        }
    }
}