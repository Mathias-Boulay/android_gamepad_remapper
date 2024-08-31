package fr.spse.gamepad_remapper;

import static android.view.InputDevice.KEYBOARD_TYPE_ALPHABETIC;
import static android.view.InputDevice.SOURCE_DPAD;
import static android.view.InputDevice.SOURCE_GAMEPAD;
import static android.view.InputDevice.SOURCE_JOYSTICK;
import static android.view.KeyEvent.KEYCODE_UNKNOWN;
import static android.view.MotionEvent.AXIS_HAT_X;
import static android.view.MotionEvent.AXIS_HAT_Y;
import static android.view.MotionEvent.AXIS_RZ;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;
import static android.view.MotionEvent.AXIS_Z;
import static fr.spse.gamepad_remapper.Remapper.AXIS_NONE;
import static fr.spse.gamepad_remapper.RemapperUtils.buttonLabel;
import static fr.spse.gamepad_remapper.RemapperUtils.dpadLabel;
import static fr.spse.gamepad_remapper.RemapperUtils.joystickButtonLabel;
import static fr.spse.gamepad_remapper.RemapperUtils.joystickLabel;
import static fr.spse.gamepad_remapper.RemapperUtils.shoulderLabel;
import static fr.spse.gamepad_remapper.RemapperUtils.triggerLabel;
import static fr.spse.gamepad_remapper.Settings.SUPPORTED_AXIS;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class RemapperView extends TextView {

    /* Visual parameters */
    private static final float CORNER_RADIUS = 15f;
    private static final int VERTICAL_MARGIN = 40;
    private static final int HORIZONTAL_MARGIN = 40;

    /* Map from one input to another */
    private final Map<Integer, Integer> inputMapKeys = new ArrayMap<>();
    private final Map<Integer, Integer> inputMapMotions = new ArrayMap<>();
    /* Colors nicely used to display state */
    private final int backgroundColor, disabledColor, enabledColor;

    private final ValueAnimator animator = new ValueAnimator();
    private final ValueAnimator reverseAnimator = new ValueAnimator();
    /* Array of inputs to remap, initialized by the $Builder */
    protected List<Integer> inputList = new ArrayList<>();
    /* All drawables that will be showed during the binding phase */
    protected List<Integer> drawableList = new ArrayList<>();
    /* All strings that will be displayed during the binding phase */
    protected List<String> textList = new ArrayList<>();
    /* The dialog hosting the view, has to be dismissed upon full binding */
    private Dialog dialog;
    /* Whether or not the view is listening to events */
    private boolean isListening = false;
    /* Points to whatever input has to be mapped */
    private int index = -1;
    /* Allow to pass down the mapper */
    private Listener listener;
    private Drawable mCurrentIconDrawable = null;
    private float dotXPos = 0; // Used for the enabled dot


    /**
     * Only meant to be used through the $Builder class
     */
    public RemapperView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        setPadding(0, 0, 0, 80 + VERTICAL_MARGIN);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();

        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        enabledColor = typedValue.data;
        disabledColor = Color.LTGRAY;
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        backgroundColor = typedValue.data;


        animator.setInterpolator(new LinearInterpolator());
        animator.setFloatValues(0, 1f);
        animator.setDuration(1200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private boolean halfPassed = false;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = animation.getAnimatedFraction();

                float slice = (getWidth() * 0.66f) / inputList.size();
                float offset = inputList.size() % 2 == 0 ? slice / 2f : 0;
                dotXPos = slice * Math.max(index + value - 1, 0) + (getWidth() * 0.165f + offset);

                if (mCurrentIconDrawable != null && index > 0) {
                    if (value < 0.5) {
                        halfPassed = false;
                        mCurrentIconDrawable.setAlpha((int) (255 * (1 - 2 * value)));
                    } else {
                        if (!halfPassed) {
                            halfPassed = true;
                            mCurrentIconDrawable.setAlpha(255);
                            mCurrentIconDrawable = getResources().getDrawable(drawableList.get(index));
                        }
                        mCurrentIconDrawable.setAlpha((int) (255 * value));
                    }

                }
                invalidate();
            }
        });

        reverseAnimator.setInterpolator(new LinearInterpolator());
        reverseAnimator.setFloatValues(0, 1F);
        reverseAnimator.setDuration(1200);
        reverseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private boolean halfPassed = false;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = animation.getAnimatedFraction();

                float slice = (getWidth() * 0.66f) / inputList.size();
                float offset = inputList.size() % 2 == 0 ? slice / 2f : 0;
                dotXPos = slice * Math.max(index - value + 1, 0) + (getWidth() * 0.165f + offset);

                if (mCurrentIconDrawable != null && index >= 0) {
                    if (value < 0.5) {
                        halfPassed = false;
                        mCurrentIconDrawable.setAlpha((int) (255 * (1 - 2 * value)));
                    } else {
                        if (!halfPassed) {
                            halfPassed = true;
                            mCurrentIconDrawable.setAlpha(255);
                            mCurrentIconDrawable = getResources().getDrawable(drawableList.get(index));
                        }
                        mCurrentIconDrawable.setAlpha((int) (255 * value));
                    }

                }
                invalidate();
            }
        });

        post(this::init);
    }

    private static int findTriggeredAxis(MotionEvent event) {
        for (int axis : SUPPORTED_AXIS) {
            if (event.getAxisValue(axis) >= 0.85) {
                return axis;
            }
        }
        return AXIS_NONE;
    }

    public static boolean isGamepadMotionEvent(MotionEvent event) {
        return event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.getAction() == MotionEvent.ACTION_MOVE;
    }

    private static boolean isDpadKeyEvent(KeyEvent event) {
        //return ((event.getSource() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) && (event.getDevice().getKeyboardType() == KEYBOARD_TYPE_NON_ALPHABETIC);
        return (event.isFromSource(SOURCE_GAMEPAD) && event.isFromSource(SOURCE_DPAD))
                && event.getDevice().getKeyboardType() != KEYBOARD_TYPE_ALPHABETIC;
    }

    public static boolean isGamepadKeyEvent(KeyEvent event) {
        boolean isGamepad = event.isFromSource(SOURCE_GAMEPAD)
                || (event.getDevice() != null && event.getDevice().supportsSource(SOURCE_GAMEPAD));

        return isGamepad || isDpadKeyEvent(event);
    }

    /**
     * Attempts to detect if the device is a gamepad.
     */
    private static boolean isGamepadDevice(InputDevice device) {
        if (device == null) return false;
        return device.supportsSource(InputDevice.SOURCE_GAMEPAD)
                || device.supportsSource(SOURCE_JOYSTICK);
    }

    private void init() {
        // First drawable
        mCurrentIconDrawable = getResources().getDrawable(drawableList.get(0));

        isListening = true;
        incrementMappedPointer();
        isListening = false;

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                System.out.println("Dialog dismissed !");
                listener.onRemapDone(null);
            }
        });

        setOnGenericMotionListener(new OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                //Toast.makeText(getContext(), "remapper view listen", Toast.LENGTH_SHORT).show();
                // First, filter potentially unwanted events
                if (!isListening) return true;
                if (isGamepadDevice(motionEvent.getDevice()) || isGamepadMotionEvent(motionEvent)) {

                    int axis = findTriggeredAxis(motionEvent);
                    // HAT axis will be captured as key events
                    if (axis == AXIS_NONE) return true;
                    inputMapMotions.put(axis, inputList.get(index));

                    incrementMappedPointer();
                }

                return true;
            }
        });

        setOnFocusChangeListener((v, hasFocus) -> requestFocus());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setFocusedByDefault(true);
        }
        setFocusable(true);
        post(this::requestFocus);
        requestFocus();
        postDelayed(() -> {
            isListening = true;
        }, 700);
    }

    /**
     * Moved outside and called by the dialog
     */
    public boolean onKey(int i, KeyEvent keyEvent) {
        // First, filter potentially unwanted events
        if (!isListening) return true;
        if (keyEvent.getRepeatCount() > 0) return true;
        if (keyEvent.getKeyCode() == KEYCODE_UNKNOWN) return true;
        if (isGamepadDevice(keyEvent.getDevice()) || isGamepadKeyEvent(keyEvent)) {

            inputMapKeys.put(keyEvent.getKeyCode(), inputList.get(index));


            incrementMappedPointer();
        }

        return true;
    }

    private void incrementMappedPointer() {
        if (index < inputList.size() - 1) {
            if (isListening) {
                ++index;

                animator.start();
                postDelayed(() -> isListening = true, 700);
                setText(textList.get(index));
            }
        } else {
            listener.onRemapDone(new Remapper(inputMapKeys, inputMapMotions));
            destroy();
        }

        isListening = false;
    }

    private void decrementMappedPointer() {
        if (index > 0) {
            --index;
            isListening = false;

            reverseAnimator.start();
            postDelayed(() -> isListening = true, 700);
            setText(textList.get(index));
        }
    }

    /**
     * Make the view disappear and out of focus
     */
    private void destroy() {
        setFocusable(false);
        dialog.setOnDismissListener(null);
        dialog.dismiss();
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();

        // Draw the focused window
        paint.setAlpha(255);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(HORIZONTAL_MARGIN, 0, getWidth() - HORIZONTAL_MARGIN, getHeight() - VERTICAL_MARGIN,
                CORNER_RADIUS, CORNER_RADIUS, paint);

        // Draw small circles displaying where the user is
        paint.setStrokeWidth(20);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(disabledColor);
        float yPos = getHeight() - (getPaddingBottom() + VERTICAL_MARGIN) / 2f;
        float slice = (getWidth() * 0.66f) / inputList.size();
        float offset = inputList.size() % 2 == 0 ? slice / 2f : 0;

        for (int i = 0; i < inputList.size(); ++i) {  // Disabled dots
            float xPos = slice * i;
            xPos += getWidth() * 0.165f + offset;
            canvas.drawPoint(xPos, yPos, paint);
        }
        paint.setColor(enabledColor);
        canvas.drawPoint(dotXPos, yPos, paint);

        // Draw the actual control icon
        if (mCurrentIconDrawable != null) {
            mCurrentIconDrawable.setBounds(getWidth() / 2 - 100, (int) ((0.4 * getHeight()) - 100), getWidth() / 2 + 100, (int) ((0.4 * getHeight()) + 100));
            mCurrentIconDrawable.draw(canvas);
        }

        super.draw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }


    public interface Listener {
        /**
         * When the remapping is done, a Remapper is built and passed down the method
         * Upon cancellation, the remapped passed is null
         */
        void onRemapDone(Remapper remapper);
    }

    /**
     * Simple Builder for the RemapperView class
     */
    static public class Builder {
        private boolean remapLeftJoystick, remapRightJoystick,
                remapLeftJoystickButton, remapRightJoystickButton, remapDpad,
                remapLeftShoulder, remapRightShoulder,
                remapLeftTrigger, remapRightTrigger,
                remapA, remapX, remapY, remapB,
                remapStart, remapSelect;

        private Listener listener;

        /**
         * @param listener The listener to which the Remapper object is passed after remapping
         */
        public Builder(Listener listener) {
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

        public Builder setRemapListener(Listener listener) {
            this.listener = listener;
            return this;
        }



        /**
         * Build and display the remapping dialog with all the parameters set previously
         *
         * @param context A context object referring to the current window
         */
        public RemapperView build(Context context) {
            final Resources resources = context.getResources();
            View fullView = LayoutInflater.from(context).inflate(R.layout.remapper_view, null);
            RemapperView view = fullView.findViewById(R.id.remapper_view);
            view.listener = listener;

            if (remapA) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_A);
                view.drawableList.add(R.drawable.button_a);
                view.textList.add(buttonLabel(resources, R.string.button_a));
            }
            if (remapB) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_B);
                view.drawableList.add(R.drawable.button_b);
                view.textList.add(buttonLabel(resources, R.string.button_b));
            }
            if (remapX) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_X);
                view.drawableList.add(R.drawable.button_x);
                view.textList.add(buttonLabel(resources, R.string.button_x));
            }
            if (remapY) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_Y);
                view.drawableList.add(R.drawable.button_y);
                view.textList.add(buttonLabel(resources, R.string.button_y));
            }
            if (remapStart) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_START);
                view.drawableList.add(R.drawable.button_start);
                view.textList.add(buttonLabel(resources, R.string.button_start));
            }
            if (remapSelect) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_SELECT);
                view.drawableList.add(R.drawable.button_select);
                view.textList.add(buttonLabel(resources, R.string.button_select));
            }
            if (remapLeftJoystick) {
                view.inputList.add(AXIS_X);
                view.inputList.add(AXIS_Y);
                view.drawableList.add(R.drawable.stick_left);
                view.textList.add(joystickLabel(resources, R.string.bind_process_left, R.string.bind_process_right));
                view.drawableList.add(R.drawable.stick_left);
                view.textList.add(joystickLabel(resources, R.string.bind_process_left, R.string.bind_process_bottom));
            }
            if (remapLeftJoystickButton) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBL);
                view.drawableList.add(R.drawable.stick_left_click);
                view.textList.add(joystickButtonLabel(resources, R.string.bind_process_left));
            }
            if (remapRightJoystick) {
                view.inputList.add(AXIS_Z);
                view.inputList.add(AXIS_RZ);
                view.drawableList.add(R.drawable.stick_right);
                view.textList.add(joystickLabel(resources, R.string.bind_process_right, R.string.bind_process_right));
                view.drawableList.add(R.drawable.stick_right);
                view.textList.add(joystickLabel(resources, R.string.bind_process_right, R.string.bind_process_bottom));
            }
            if (remapRightJoystickButton) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_THUMBR);
                view.drawableList.add(R.drawable.stick_right_click);
                view.textList.add(joystickButtonLabel(resources, R.string.bind_process_right));
            }
            if (remapLeftShoulder) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_L1);
                view.drawableList.add(R.drawable.shoulder_left);
                view.textList.add(shoulderLabel(resources, R.string.bind_process_left));
            }
            if (remapRightShoulder) {
                view.inputList.add(KeyEvent.KEYCODE_BUTTON_R1);
                view.drawableList.add(R.drawable.shoulder_right);
                view.textList.add(shoulderLabel(resources, R.string.bind_process_right));
            }
            if (remapLeftTrigger) {
                view.inputList.add(MotionEvent.AXIS_LTRIGGER);
                view.drawableList.add(R.drawable.trigger_left);
                view.textList.add(triggerLabel(resources, R.string.bind_process_left));
            }
            if (remapRightTrigger) {
                view.inputList.add(MotionEvent.AXIS_RTRIGGER);
                view.drawableList.add(R.drawable.trigger_right);
                view.textList.add(triggerLabel(resources, R.string.bind_process_right));
            }
            if (remapDpad) {
                view.inputList.add(AXIS_HAT_X);
                view.inputList.add(AXIS_HAT_Y);
                view.drawableList.add(R.drawable.dpad_right);
                view.drawableList.add(R.drawable.dpad_down);
                view.textList.add(dpadLabel(resources, R.string.bind_process_right));
                view.textList.add(dpadLabel(resources, R.string.bind_process_down));
            }

            final ImageButton backButton = fullView.findViewById(R.id.back_button);
            final Button skipButton = fullView.findViewById(R.id.skip_button);

            // Once the view is built, display it via an alert dialog
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(fullView)
                    .setCancelable(false)
                    .create();

            view.dialog = dialog;
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            dialog.setOnShowListener(d -> {
                dialog.getWindow().getDecorView().requestFocus();
                backButton.setFocusable(false);
                backButton.setFocusableInTouchMode(false);

                backButton.setOnClickListener(v -> {
                    view.decrementMappedPointer();
                    view.requestFocus(View.FOCUS_BACKWARD);
                });

                skipButton.setFocusable(false);
                skipButton.setFocusableInTouchMode(false);
                skipButton.setOnClickListener(v -> {
                    view.incrementMappedPointer();
                    view.requestFocus(View.FOCUS_BACKWARD);
                });
            });


            dialog.show();
            fullView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (context.getResources().getDisplayMetrics().heightPixels * 0.9)));
            fullView.requestFocus();
            fullView.postDelayed(fullView::requestFocus, 500);

            dialog.setOnKeyListener((dialog1, keyCode, event) -> view.onKey(keyCode, event));


            return view;
        }
    }
}
