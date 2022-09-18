package fr.spse.gamepad_remapper;

import static android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC;
import static android.view.InputDevice.SOURCE_DPAD;
import static android.view.InputDevice.SOURCE_GAMEPAD;
import static android.view.InputDevice.SOURCE_JOYSTICK;
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
import static fr.spse.gamepad_remapper.Remapper.transformKeyEventInput;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class RemapperView extends TextView {

    /* Whether or not the view is listening to events */
    private boolean isListening = true;

    /* Map from one input to another */
    private final Map<Integer, Integer> inputMap = new ArrayMap<>();

    /* Array of inputs to remap, initialized by the $Builder */
    protected List<Integer> inputList = new ArrayList<>();

    /* Points to whatever input has to be mapped */
    private int index = -1;

    /* Allow to pass down the mapper */
    private Listener listener;


    /** Only meant to be used through the $Builder class */
    public RemapperView(ViewGroup parent, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(parent.getContext(), attrs, defStyleAttr, defStyleRes);
        // Auto display yourself
        parent.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
                    inputMap.put(transformKeyEventInput(keyEvent.getKeyCode()),inputList.get(index));


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
                    inputMap.put(axis, inputList.get(index));

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

        setFocusable(true);
        requestFocus();
    }

    private void incrementMappedPointer(){
        if(index < inputList.size()-1){
            if(isListening){
                ++index;

                postDelayed(() -> isListening = true, 1000);
                setText("Listening for " + KeyEvent.keyCodeToString(inputList.get(index)) + "/" + MotionEvent.axisToString(inputList.get(index)));
            }
        }else{
            listener.onRemapDone(new Remapper(inputMap));
            destroy();
        }

        isListening = false;
    }

    /** Make the view disappear and out of focus */
    private void destroy(){
        setFocusable(false);
        ((ViewGroup) getParent()).removeView(this);
    }

    private static int findTriggeredAxis(MotionEvent event){
        for(int axis : new int[]{AXIS_RX, AXIS_RY, AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_BRAKE, AXIS_THROTTLE, AXIS_RTRIGGER, AXIS_LTRIGGER}){
            if(event.getAxisValue(axis) >= 0.5){
                return axis;
            }
        }
        return AXIS_NONE;
    }




    private static boolean isGamepadMotionEvent(MotionEvent event){
        return event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.getAction() == MotionEvent.ACTION_MOVE;
    }

    public static boolean isDpadKeyEvent(KeyEvent event){
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

        private Listener listener;

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
        public RemapperView build(ViewGroup parent, AttributeSet attrs, int defStyleAttr, int defStyleRes){
            RemapperView view = new RemapperView(parent, attrs, defStyleAttr, defStyleRes);
            view.listener = listener;

            if(remapA) view.inputList.add(KeyEvent.KEYCODE_BUTTON_A);
            if(remapB) view.inputList.add(KeyEvent.KEYCODE_BUTTON_B);
            if(remapX) view.inputList.add(KeyEvent.KEYCODE_BUTTON_X);
            if(remapY) view.inputList.add(KeyEvent.KEYCODE_BUTTON_Y);
            if(remapStart) view.inputList.add(KeyEvent.KEYCODE_BUTTON_START);
            if(remapSelect) view.inputList.add(KeyEvent.KEYCODE_BUTTON_SELECT);
            if(remapLeftJoystick){
                view.inputList.add(AXIS_X);
                view.inputList.add(AXIS_Y);
            }
            /*
            if(remapLeftJoystick){
                view.inputList.add(LEFT_JOYSTICK_UP);
                view.inputList.add(LEFT_JOYSTICK_RIGHT);
                view.inputList.add(LEFT_JOYSTICK_DOWN);
                view.inputList.add(LEFT_JOYSTICK_LEFT);
            }

             */
            if(remapLeftJoystickButton) view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBL);
            /*
            if(remapRightJoystick){
                view.inputList.add(RIGHT_JOYSTICK_UP);
                view.inputList.add(RIGHT_JOYSTICK_RIGHT);
                view.inputList.add(RIGHT_JOYSTICK_DOWN);
                view.inputList.add(RIGHT_JOYSTICK_LEFT);
            }
             */
            if(remapRightJoystick){
                view.inputList.add(AXIS_Z);
                view.inputList.add(AXIS_RZ);
            }
            if(remapRightJoystickButton) view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBR);
            if(remapLeftShoulder) view.inputList.add(KeyEvent.KEYCODE_BUTTON_L1);
            if(remapRightShoulder) view.inputList.add(KeyEvent.KEYCODE_BUTTON_R1);
            if(remapLeftTrigger) view.inputList.add(MotionEvent.AXIS_LTRIGGER);
            if(remapRightTrigger) view.inputList.add(MotionEvent.AXIS_RTRIGGER);
            if(remapDpad){
                view.inputList.add(KEYCODE_DPAD_UP);
                view.inputList.add(KEYCODE_DPAD_RIGHT);
                view.inputList.add(KEYCODE_DPAD_DOWN);
                view.inputList.add(KEYCODE_DPAD_LEFT);
            }

            return view;
        }
    }
}
