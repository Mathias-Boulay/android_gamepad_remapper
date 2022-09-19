package fr.spse.gamepad_remapper;

import static android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC;
import static android.view.InputDevice.SOURCE_DPAD;
import static android.view.InputDevice.SOURCE_GAMEPAD;
import static android.view.InputDevice.SOURCE_JOYSTICK;
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

import static fr.spse.gamepad_remapper.Remapper.AXIS_NONE;
import static fr.spse.gamepad_remapper.Remapper.DPAD_CENTER;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class RemapperView extends TextView {

    /* The dialog hosting the view, has to be dismissed upon full binding */
    private DialogInterface dialogInterface;

    /* Whether or not the view is listening to events */
    private boolean isListening = true;

    /* Map from one input to another */
    private final Map<Integer, Integer> inputMapKeys = new ArrayMap<>();
    private final Map<Integer, Integer> inputMapMotions = new ArrayMap<>();

    /* Array of inputs to remap, initialized by the $Builder */
    protected List<Integer> inputList = new ArrayList<>();

    /* All drawables that will be showed during the binding phase */
    protected List<Integer> drawableList = new ArrayList<>();

    /* All strings that will be displayed during the binding phase */
    protected List<Integer> textList = new ArrayList<>();

    /* Points to whatever input has to be mapped */
    private int index = -1;

    /* Allow to pass down the mapper */
    private Listener listener;

    /* Colors nicely used to display state */
    private final int backgroundColor, disabledColor, enabledColor;

    private Drawable mCurrentIconDrawable = null;

    /* Visual parameters */
    private final float cornerRadius = 15f;
    private final int verticalMargin = 40;
    private final int horizontalMargin = 40;


    /** Only meant to be used through the $Builder class */
    public RemapperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Auto display yourself
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(500, 500);
        //parent.addView(this, params);
        setLayoutParams(params);
        setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);
        setPadding(0, 0 ,0, 80 + verticalMargin);


        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();

        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        enabledColor = typedValue.data;
        disabledColor = Color.LTGRAY;
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        backgroundColor = typedValue.data;

        setElevation(25);

        post(this::init);
    }


    private void init(){
        incrementMappedPointer();

        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                // First, filter potentially unwanted events
                if(!isListening) return true;
                if(keyEvent.getRepeatCount() > 0) return true;
                if(keyEvent.getKeyCode() == KEYCODE_UNKNOWN) return true;
                if(isGamepadDevice(keyEvent.getDevice()) || isGamepadKeyEvent(keyEvent)){
                    //TODO handle the keyevent
                    inputMapKeys.put(keyEvent.getKeyCode(),inputList.get(index));


                    incrementMappedPointer();
                }

                return true;
            }
        });


        setOnGenericMotionListener(new OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                // First, filter potentially unwanted events
                if(!isListening) return true;
                if(isGamepadDevice(motionEvent.getDevice()) || isGamepadMotionEvent(motionEvent)){

                    int axis = findTriggeredAxis(motionEvent);
                    // HAT axis will be captured as key events
                    if(axis == AXIS_NONE) return true;
                    //TODO handle the keyevent
                    inputMapMotions.put(axis, inputList.get(index));

                    incrementMappedPointer();
                }

                return true;
            }
        });

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) v.requestFocus();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setFocusedByDefault(true);
        }
        setFocusable(true);
        requestFocus();
    }

    private void incrementMappedPointer(){
        if(index < inputList.size()-1){
            if(isListening){
                ++index;

                postDelayed(() -> isListening = true, 800);
                setText(getResources().getString(textList.get(index)));
                mCurrentIconDrawable = getResources().getDrawable(drawableList.get(index));
            }
        }else{
            listener.onRemapDone(new Remapper(inputMapKeys, inputMapMotions));
            destroy();
        }

        isListening = false;
    }

    /** Make the view disappear and out of focus */
    private void destroy(){
        setFocusable(false);
        dialogInterface.dismiss();
    }

    private static int findTriggeredAxis(MotionEvent event){
        for(int axis : new int[]{AXIS_HAT_X, AXIS_HAT_Y, AXIS_RX, AXIS_RY, AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_BRAKE, AXIS_THROTTLE, AXIS_RTRIGGER, AXIS_LTRIGGER}){
            if(event.getAxisValue(axis) >= 0.85){
                return axis;
            }
        }
        return AXIS_NONE;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();

        // Draw the focused window
        paint.setAlpha(255);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(horizontalMargin, 2*getHeight()/3f, getWidth() - horizontalMargin, getHeight() - verticalMargin,
                cornerRadius, cornerRadius, paint);

        // Draw small circles displaying where the user is
        paint.setStrokeWidth(20);
        paint.setStrokeCap(Paint.Cap.ROUND);
        float yPos = getHeight() - (getPaddingBottom() + verticalMargin)/2f;
        float slice = (getWidth() * 0.66f)/inputList.size();
        float offset = inputList.size() % 2 == 0 ? slice/2f : 0;

        for(int i=0; i<inputList.size(); ++i){
            float xPos = slice * i;
            xPos += getWidth() * 0.165f + offset;

            // Set appropriate paint color
            paint.setColor(i == index ? enabledColor : disabledColor);

            canvas.drawPoint(xPos, yPos, paint);
        }

        // Draw the actual control icon
        if(mCurrentIconDrawable != null){
            mCurrentIconDrawable.setBounds(getWidth()/2-100, (int) ((0.79f * getHeight()) - 100), getWidth()/2 + 100 , (int) ((0.79f * getHeight()) + 100));
            mCurrentIconDrawable.draw(canvas);
        }

        super.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public static boolean isGamepadMotionEvent(MotionEvent event){
        return event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.getAction() == MotionEvent.ACTION_MOVE;
    }

    private static boolean isDpadKeyEvent(KeyEvent event){
        //return ((event.getSource() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) && (event.getDevice().getKeyboardType() == KEYBOARD_TYPE_NON_ALPHABETIC);
        return (event.isFromSource(SOURCE_GAMEPAD) && event.isFromSource(SOURCE_DPAD))
                && event.getDevice().getKeyboardType() != KEYBOARD_TYPE_ALPHABETIC;
    }

    public static boolean isGamepadKeyEvent(KeyEvent event){
        boolean isGamepad = event.isFromSource(SOURCE_GAMEPAD)
                || (event.getDevice() != null && event.getDevice().supportsSource(SOURCE_GAMEPAD));

        return isGamepad || isDpadKeyEvent(event);
    }

    /** Attempts to detect if the device is a gamepad. */
    private static boolean isGamepadDevice(InputDevice device) {
        if (device == null) return false;
        return device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                || device.supportsSource(SOURCE_JOYSTICK);
    }



    public interface Listener{
        /** When the remapping is done, a remapper is built and passed down the method */
        void onRemapDone(Remapper remapper);
    }

    /** Simple Builder for the RemapperView class */
    static public class Builder {
        private boolean remapLeftJoystick, remapRightJoystick,
                remapLeftJoystickButton,remapRightJoystickButton, remapDpad,
                remapLeftShoulder, remapRightShoulder,
                remapLeftTrigger, remapRightTrigger,
                remapA, remapX, remapY, remapB,
                remapStart, remapSelect;

        private final Listener listener;

        /** The listener is required to handle when the remapping is done */
        public Builder(Listener listener){
            this.listener = listener;
        }

        public Builder remapLeftJoystick(boolean remapLeftJoystick) {
            this.remapLeftJoystick = remapLeftJoystick;
            return this;
        }

        public Builder remapRightJoystick(boolean remapRightJoystick) {
            this.remapRightJoystick = remapRightJoystick;
            return this;
        }

        public Builder remapLeftJoystickButton(boolean remapLeftJoystickButton) {
            this.remapLeftJoystickButton = remapLeftJoystickButton;
            return this;
        }

        public Builder remapRightJoystickButton(boolean remapRightJoystickButton) {
            this.remapRightJoystickButton = remapRightJoystickButton;
            return this;
        }

        public Builder remapDpad(boolean remapDpad) {
            this.remapDpad = remapDpad;
            return this;
        }

        public Builder remapLeftShoulder(boolean remapLeftShoulder) {
            this.remapLeftShoulder = remapLeftShoulder;
            return this;
        }

        public Builder remapRightShoulder(boolean remapRightShoulder) {
            this.remapRightShoulder = remapRightShoulder;
            return this;
        }

        public Builder remapLeftTrigger(boolean remapLeftTrigger) {
            this.remapLeftTrigger = remapLeftTrigger;
            return this;
        }

        public Builder remapRightTrigger(boolean remapRightTrigger) {
            this.remapRightTrigger = remapRightTrigger;
            return this;
        }

        public Builder remapA(boolean remapA) {
            this.remapA = remapA;
            return this;
        }

        public Builder remapX(boolean remapX) {
            this.remapX = remapX;
            return this;
        }

        public Builder remapY(boolean remapY) {
            this.remapY = remapY;
            return this;
        }

        public Builder remapB(boolean remapB) {
            this.remapB = remapB;
            return this;
        }

        public Builder remapStart(boolean remapStart) {
            this.remapStart = remapStart;
            return this;
        }

        public Builder remapSelect(boolean remapSelect) {
            this.remapSelect = remapSelect;
            return this;
        }

        /** Build the view with the max amount of attributes, and set all the mapping to be done */
        public void build(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
            View fullView = LayoutInflater.from(context).inflate(R.layout.remapper_view, null);


            RemapperView view = fullView.findViewById(R.id.remapper_view);
            view.listener = listener;

            if(remapA){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_A);
                view.drawableList.add(R.drawable.button_a);
                view.textList.add(R.string.bind_process_press_a);
            }
            if(remapB){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_B);
                view.drawableList.add(R.drawable.button_b);
                view.textList.add(R.string.bind_process_press_b);
            }
            if(remapX){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_X);
                view.drawableList.add(R.drawable.button_x);
                view.textList.add(R.string.bind_process_press_x);
            }
            if(remapY){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_Y);
                view.drawableList.add(R.drawable.button_y);
                view.textList.add(R.string.bind_process_press_y);
            }
            if(remapStart){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_START);
                view.drawableList.add(R.drawable.button_start);
                view.textList.add(R.string.bind_process_press_start);
            }
            if(remapSelect){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_SELECT);
                view.drawableList.add(R.drawable.button_select);
                view.textList.add(R.string.bind_process_press_select);
            }
            if(remapLeftJoystick){
                view.inputList.add(AXIS_X);
                view.inputList.add(AXIS_Y);
                view.drawableList.add(R.drawable.stick_left);
                view.textList.add(R.string.bind_process_move_stick_left_x);
                view.drawableList.add(R.drawable.stick_left);
                view.textList.add(R.string.bind_process_move_stick_left_y);
            }
            if(remapLeftJoystickButton){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBL);
                view.drawableList.add(R.drawable.stick_left_click);
                view.textList.add(R.string.bind_process_press_stick_left);
            }
            if(remapRightJoystick){
                view.inputList.add(AXIS_Z);
                view.inputList.add(AXIS_RZ);
                view.drawableList.add(R.drawable.stick_right);
                view.textList.add(R.string.bind_process_move_stick_right_x);
                view.drawableList.add(R.drawable.stick_right);
                view.textList.add(R.string.bind_process_move_stick_right_y);
            }
            if(remapRightJoystickButton){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBR);
                view.drawableList.add(R.drawable.stick_right_click);
                view.textList.add(R.string.bind_process_press_stick_right);
            }
            if(remapLeftShoulder){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_L1);
                view.drawableList.add(R.drawable.shoulder_left);
                view.textList.add(R.string.bind_process_press_shoulder_left);
            }
            if(remapRightShoulder){
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_R1);
                view.drawableList.add(R.drawable.shoulder_right);
                view.textList.add(R.string.bind_process_press_shoulder_right);
            }
            if(remapLeftTrigger){
                view.inputList.add(MotionEvent.AXIS_LTRIGGER);
                view.drawableList.add(R.drawable.trigger_left);
                view.textList.add(R.string.bind_process_press_trigger_left);
            }
            if(remapRightTrigger){
                view.inputList.add(MotionEvent.AXIS_RTRIGGER);
                view.drawableList.add(R.drawable.trigger_right);
                view.textList.add(R.string.bind_process_press_trigger_right);
            }
            if(remapDpad){
                //view.inputList.add(KEYCODE_DPAD_UP);
                view.inputList.add(AXIS_HAT_X);
                view.inputList.add(AXIS_HAT_Y);
                //view.inputList.add(KEYCODE_DPAD_LEFT);
                //view.drawableList.add(R.drawable.dpad_up);
                view.drawableList.add(R.drawable.dpad_right);
                view.drawableList.add(R.drawable.dpad_down);
                //view.drawableList.add(R.drawable.dpad_left);
                //view.textList.add(R.string.bind_process_press_dpad_up);
                view.textList.add(R.string.bind_process_press_dpad_right);
                view.textList.add(R.string.bind_process_press_dpad_down);
                //view.textList.add(R.string.bind_process_press_dpad_left);
            }


            // Once the view is built, display it via an alert dialog
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(fullView)
                    .setCancelable(false)
                    .create();

            view.dialogInterface = dialog;

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            fullView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (context.getResources().getDisplayMetrics().heightPixels * 0.9)));

            fullView.requestFocus();
            fullView.postDelayed(() -> fullView.requestFocus(), 500);

        }
    }
}
