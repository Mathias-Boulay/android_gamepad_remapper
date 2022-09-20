package fr.spse.gamepad_remapper;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.json.JSONException;

/**
 * Manager class to streamline even more the integration of gamepads
 * It auto handles displaying the mapper view and handling events.
 *
 * Note that the compatibility with a manual integration at the same time is limited
 */
public class RemapperManager {
    private final RemapperView.Builder builder;
    private Remapper remapper;
    private RemapperView remapperView;

    /**
     * @param context A context for SharedPreferences. The Manager attempts to fetch an existing remapper.
     * @param builder Builder with all the params set in. Note that the listener is going to be overridden.
     */
    public RemapperManager(Context context, RemapperView.Builder builder){
        try {
            remapper = new Remapper(context);
        } catch (JSONException e) {
            Log.e("RemapperManager", "Could not load from library !");
            remapper = null;
        }
        this.builder = builder;
    }

    /**
     * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method
     * Will automatically ask to remap if no remapper is available
     * @param event The current MotionEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleMotionEventInput(Context context, MotionEvent event, GamepadHandler handler){
        if(buildView(context)) return true;
        return remapper.handleMotionEventInput(event, handler);
    }

    /**
     * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method
     * Will automatically ask to remap if no remapper is available
     * @param event The current KeyEvent
     * @param handler The handler, through which remapped inputs will be passed.
     * @return Whether the input was handled or not.
     */
    public boolean handleKeyEventInput(Context context, KeyEvent event, GamepadHandler handler){
        if(buildView(context)) return true;
        return remapper.handleKeyEventInput(event, handler);
    }

    /** @return True if the RemapperView has just been built or displayed, waiting for a remapper */
    private boolean buildView(Context context){
        if(remapper != null) return false;
        if(remapperView != null) return true;

        builder.setRemapListener(new RemapperView.Listener() {
            @Override
            public void onRemapDone(Remapper remapper) {
                remapperView = null; // Destroy the reference, we don't want to always keep the view
                if(remapper == null){
                    return;
                }
                RemapperManager.this.remapper = remapper;
                remapper.save(context); // TODO async ?
            }
        });
        remapperView = builder.build(context);
        return true;
    }
}
