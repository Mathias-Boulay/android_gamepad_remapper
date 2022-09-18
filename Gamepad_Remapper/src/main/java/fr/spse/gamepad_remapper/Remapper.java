package fr.spse.gamepad_remapper;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.MotionEvent.ACTION_BUTTON_PRESS;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.AXIS_BRAKE;
import static android.view.MotionEvent.AXIS_HAT_X;
import static android.view.MotionEvent.AXIS_HAT_Y;
import static android.view.MotionEvent.AXIS_LTRIGGER;
import static android.view.MotionEvent.AXIS_RTRIGGER;
import static android.view.MotionEvent.AXIS_RZ;
import static android.view.MotionEvent.AXIS_THROTTLE;
import static android.view.MotionEvent.AXIS_X;
import static android.view.MotionEvent.AXIS_Y;
import static android.view.MotionEvent.AXIS_Z;

import android.content.Context;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.Map;

public class Remapper {
    public static final int AXIS_NONE = -1;

    private static final int DPAD_UP = -10;
    private static final int DPAD_RIGHT = -11;
    private static final int DPAD_DOWN = -12;
    private static final int DPAD_LEFT = -13;

    private final Map<Integer, Integer> mMap;
    private final Map<Integer, Integer> mReverseMap = new ArrayMap<>();

    public Remapper(Map<Integer, Integer> remappingMap){
        mMap = remappingMap;
        for(Map.Entry<Integer, Integer> entry : mMap.entrySet()){
            mReverseMap.put(entry.getValue(), entry.getKey());
        }
    }


    /** If remapped, get the mapped source from keyEvent */
    public int getRemappedSource(KeyEvent event){
        Integer mappedValue = mMap.get(transformKeyEventInput(event.getKeyCode()));
        return mappedValue == null ? event.getKeyCode() : mappedValue;
    }

    /** If remapped, get the mapped source from MotionEvent */
    public int getRemappedSource(MotionEvent event, int axisSource){
        Integer mappedValue = mMap.get(axisSource);
        return mappedValue == null ? axisSource : mappedValue;
    }

    /** Get the converted value for the given mapped source */
    //TODO side effect with clashing values
    public float getRemappedValue(int mappedSource, KeyEvent keyEvent){
        if(isAxis(mappedSource)){
            if(keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE){
                return 1f;
            }
            return 0f;
        }
        // Else we assume it is a simple button;
        return keyEvent.getAction();
    }

    /** Get the converted value for the given mapped source */
    public float getRemappedValue(int mappedSource, MotionEvent motionEvent){
        if(isAxis(mappedSource)){
            return motionEvent.getAxisValue(mReverseMap.get(mappedSource));
        }

        // Else, convert to a keyEvent action
        return Math.abs(motionEvent.getAxisValue(mReverseMap.get(mappedSource))) >= 0.5
                ? ACTION_DOWN : ACTION_UP;
    }

    /** @return Whether the input source is a **gamepad** axis. */
    private boolean isAxis(int inputSource){
        return inputSource == AXIS_X || inputSource == AXIS_Y
                || inputSource == AXIS_Z || inputSource == AXIS_RZ
                || inputSource == AXIS_BRAKE || inputSource == AXIS_THROTTLE
                || inputSource == AXIS_LTRIGGER || inputSource == AXIS_RTRIGGER
                || inputSource == AXIS_HAT_X || inputSource == AXIS_HAT_Y;


    }

    /** Changes the dpad value by another value with the same meaning.
     * It is done because some axis share the same value as KEYCODE_DPAD_XX
     * @param keycode The keycode to transform
     */
    public static int transformKeyEventInput(int keycode){
        if(keycode == KEYCODE_DPAD_UP) return DPAD_UP;
        if(keycode == KEYCODE_DPAD_RIGHT) return DPAD_RIGHT;
        if(keycode == KEYCODE_DPAD_DOWN) return DPAD_DOWN;
        if(keycode == KEYCODE_DPAD_LEFT) return DPAD_LEFT;
        return keycode;
    }
}

