package fr.spse.gamepad_remapper;

import static fr.spse.gamepad_remapper.Remapper.SHARED_PREFERENCE_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.json.JSONException;

/**
 * Manager class to streamline even more the integration of gamepads
 * It auto handles displaying the mapper view and handling events.
 * <p>
 * Note that the compatibility with a manual integration at the same time is limited
 */
public class RemapperManager {
    private final RemapperView.Builder builder;
    private ArrayMap<String, Remapper> remappers = new ArrayMap<>();
    private RemapperView remapperView;

    /**
     * @param context A context for SharedPreferences. The Manager attempts to fetch an existing remapper.
     * @param builder Builder with all the params set in. Note that the listener is going to be overridden.
     */
    public RemapperManager(Context context, RemapperView.Builder builder) {
        this.builder = builder;
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        for (String remapperKey : preferences.getAll().keySet()) {
            try {
                remappers.put(remapperKey, new Remapper(context, remapperKey));
            } catch (JSONException e) {
                Log.e(RemapperManager.class.toString(), "Could not create the following remapper:" + remapperKey);
            }
        }
    }

    /**
     * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method
     * Will automatically ask to remap if no remapper is available
     *
     * @param event   The current MotionEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleMotionEventInput(Context context, MotionEvent event, GamepadHandler handler) {
        if (buildView(context, getGamepadIdentifier(event))) return true;
        return remappers.get(getGamepadIdentifier(event)).handleMotionEventInput(event, handler);
    }

    /**
     * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method
     * Will automatically ask to remap if no remapper is available
     *
     * @param event   The current KeyEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleKeyEventInput(Context context, KeyEvent event, GamepadHandler handler) {
        if (buildView(context, getGamepadIdentifier(event))) return true;
        return remappers.get(getGamepadIdentifier(event)).handleKeyEventInput(event, handler);
    }

    /**
     * @return True if the RemapperView has just been built or displayed, waiting for a remapper
     */
    private boolean buildView(Context context, final String gamepadID) {
        if (remappers.get(gamepadID) != null) return false;
        if (remapperView != null) return true;

        builder.setRemapListener(new RemapperView.Listener() {
            @Override
            public void onRemapDone(Remapper remapper) {
                remapperView = null; // Destroy the reference, we don't want to always keep the view
                if (remapper == null) {
                    return;
                }
                RemapperManager.this.remappers.put(gamepadID, remapper);
                remapper.save(context, gamepadID); // TODO async ?
            }
        });
        remapperView = builder.build(context);
        return true;
    }

    /**
     * Wrapper for the InputDevice descriptor
     */
    private String getGamepadIdentifier(KeyEvent event) {
        return event.getDevice().getDescriptor();
    }

    /**
     * Wrapper for the InputDevice descriptor
     */
    private String getGamepadIdentifier(MotionEvent event) {
        return event.getDevice().getDescriptor();
    }
}
