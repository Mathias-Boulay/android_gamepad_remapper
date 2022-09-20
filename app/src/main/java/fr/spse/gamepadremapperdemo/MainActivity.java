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
import static android.view.MotionEvent.AXIS_HAT_X;
import static android.view.MotionEvent.AXIS_HAT_Y;
import static android.view.MotionEvent.AXIS_LTRIGGER;
import static android.view.MotionEvent.AXIS_RTRIGGER;
import static android.view.MotionEvent.AXIS_RZ;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;
import static android.view.MotionEvent.AXIS_Z;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import fr.spse.gamepad_remapper.GamepadHandler;
import fr.spse.gamepad_remapper.Remapper;
import fr.spse.gamepad_remapper.RemapperManager;
import fr.spse.gamepad_remapper.RemapperView;

public class MainActivity extends Activity implements GamepadHandler {

    private ImageView imageView;
    private RemapperManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image_view);

        // Create a builder with all the data we need.
        // The listener here is not needed, since the builder is passed to the RemapperManager
        // which handles listening by itself.
        RemapperView.Builder builder = new RemapperView.Builder(null)
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
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return manager.handleKeyEventInput(this, event, this) || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return manager.handleMotionEventInput(this, event, this) || super.dispatchGenericMotionEvent(event);
    }

    /**
     * Function handling all gamepad actions
     * @param code Either a keycode (Eg. KEYBODE_BUTTON_A), either an axis (Eg. AXIS_HAT_X)
     * @param value For keycodes, 0 for released state, 1 for pressed state.
     *              For Axis, the value of the axis. Varies between 0/1 or -1/1 depending on the axis.
     */
    public void handleGamepadInput(int code, float value){
        switch (code){
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

            // SHoulder buttons
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

    private void setImageIfPositiveValue(int imageId, float value){
        if(value >= 0.85){
            imageView.setImageDrawable(getResources().getDrawable(imageId, getTheme()));
        }
    }

    private void setImageIfAbsValue(int imageId, float value){
        if(Math.abs(value) >= 0.85){
            imageView.setImageDrawable(getResources().getDrawable(imageId, getTheme()));
        }
    }
}