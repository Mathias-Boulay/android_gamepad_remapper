package fr.spse.gamepad_remapper;


public interface GamepadHandler {

    /**
     * Function handling all gamepad actions.
     *
     * @param code  Either a keycode (Eg. KEYBODE_BUTTON_A), either an axis (Eg. AXIS_HAT_X)
     * @param value For keycodes, 0 for released state, 1 for pressed state.
     *              For Axis, the value of the axis. Varies between 0/1 or -1/1 depending on the axis.
     */
    void handleGamepadInput(int code, float value);
}
