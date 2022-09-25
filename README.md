
# G-Mapper for Android
[![](https://jitpack.io/v/Mathias-Boulay/android_gamepad_remapper.svg)](https://jitpack.io/#Mathias-Boulay/android_gamepad_remapper)


An incredibly easy to use, lightweight and widely compatible library to streamline the implementation of gamepads for android applications !

## Why this library ?
According to the  [official android regarding gamepad integration](https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input#button), you have 2 ways of capturing the input for the DPAD and triggers, either via `KeyEvent`  or `MotionEvents`.
But since Android is not standardized enough, the actual buttons/axis reported are swapped, or completely different ones !

This library addresses both issues.

## Installation
First, add the dependency inside the `build.gradle` file of your app module:

```css
implementation 'com.github.Mathias-Boulay:android_gamepad_remapper:master-SNAPSHOT'
```

If not done so already, you need to add the Jitpack repository to the root `build.gradle` file:
```css
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

## Ultra quick start
Clone this repository, and fiddle around with the demo project to see how it's made.  Also available as a release !

## Quick start - Managed integration
The managed integration passes down all of the work to the library.
Compared to the manual integration, it takes care of the following:
- Automatically show the remapping UI, **per gamepad**.
- Auto load/save of the remapping data
- Solve the third issue of vanilla gamepad integration: Having to check the value of every axis when a MotionEvent occurs by storing values.


<details>
<summary><b>How to add a managed integration</b></summary>

### Step 1: Inject behavior into the activity
Consider the following code block, which integrates the entire lib into the activity which needs to support gamepad input.

 ```java
class MyActivity extends Activity implements GamepadHandler {
		// The RemapperView.Builder object allows you to set which buttons to remap
		private RemapperManager inputManager = new RemapperManager(this, new RemapperView.Builder(null)
			.remapDpad(true)  
			.remapLeftJoystick(true)  
			.remapRightJoystick(true)
			.remapLeftTrigger(true)  
			.remapRightTrigger(true));
	
	@Override  // Redirect KeyEvents to the remapper if one is available
	public boolean dispatchKeyEvent(KeyEvent event) {  
	    return inputManager.handleKeyEventInput(this, event, this) || super.dispatchKeyEvent(event);  
	}  
  
	@Override  // Redirect MotionEvents to the remapper if one is available
	public boolean dispatchGenericMotionEvent(MotionEvent event) {  
	    return inputManager.handleMotionEventInput(this, event, this) || super.dispatchGenericMotionEvent(event);  
	}
	
	@Override // Implement the GamepadHandler interface
	public void handleGamepadInput(int code, float value){
		// TODO Your code to take care of the gamepad input.
	}
}
```
The `Activity` implements `GamepadHandler` method: `handleGamepadInput`.
See the full documentation on how to implement it for managed instances.

With that, you're done integrating the gamepad !

</details>


## Quick start - Manual Integration
When performing a manual integration, **you have** to take care of the following things:
- Displaying the UI at the right moment
- Saving and loading of **all** gamepad mappings

<details>
<summary>Manual integration</summary>

### Step 1: Display the remapping UI
To display the remapping UI to the user, use the `RemapperView.Builder` object to build the `RemapperView`:
```java
new RemapperView.Builder(
	new RemapperView.Listener() {  
	@Override
	public void onRemapDone(Remapper remapper) {
		// This method is called when the user finished remapping
		// Here, you can save the remapper instance into a file and grab a reference to it.
		}  
	})  
	.remapDpad(true)  
	.remapLeftJoystick(true)  
	.remapRightJoystick(true)
	.remapLeftTrigger(true)  
	.remapRightTrigger(true)
	.build(this);
```
Once the remapping is done, you get a `Remapper` instance passed through the `RemapperView.Listener` interface.
**Note:** The full array of remappable controls is available on the documentation below.


### Step 2: Make use of the mapped control scheme
Once the remapping is done, we can make use on the `Remapper` object.
Inside your activity supporting the gamepad:
- override 2 functions to intercept controller's `KeyEvent`  and `MotionEvent`
- Implement the `GamepadHandler` interface, which handles standardized and mapped input

```java
class MyActivity extends Activity implements GamepadHandler{
	private Remapper mRemapper;
	...
	@Override  // Redirect KeyEvents to the remapper if one is available
	public boolean dispatchKeyEvent(KeyEvent event) {  
		if(remapper == null) return super.dispatchKeyEvent(event);  
		return remapper.handleKeyEventInput(event, this);  
	}  
  
	@Override  // Redirect MotionEvents to the remapper if one is available
	public boolean dispatchGenericMotionEvent(MotionEvent event) {  
	    	if(remapper == null) return super.onGenericMotionEvent(event);  
		return remapper.handleMotionEventInput(event, this);  
	}
	
	@Override // Implement the GamepadHandler interface
	public void handleGamepadInput(int code, float value){
		// TODO Your code to take care of the gamepad input.
		// NOTE: INPUT ON AXIS WILL BE CALLED MANY TIMES, DESPITE THE VALUE NOT CHANGING ON MANUAL INTEGRATION
	}
}
```

**Note:** Inputs may be called many times despite the value from the axis not changing.
This is an issue related to the vanilla gamepad integration.

Lazier people might want to use the Managed integration.
Consult the FULL DOCUMENTATION for details.
</details>

# Full documentation
<details>
<summary>Click here to see it in all its glory !</summary>

## Remapper
Class able to map inputs from one way or another, used to normalize inputs.

### Constructors
```java
/**
 * Load the Remapper data from the shared preferences 
 * @param context A context object, necessary to fetch SharedPreferences  
 */
public Remapper(Context context);
```

### Functions
```java
/**
 * If the event is a valid Gamepad event, call the GamepadHandler method.
 * @param event The current MotionEvent  
 * @param handler The handler, through which remapped inputs will be passed.  
 * @return Whether the input was handled or not.  
 */
public boolean handleMotionEventInput(MotionEvent event, GamepadHandler handler);
```

```java
/**
 * If the event is a valid Gamepad event, call the GamepadHandler method
 * @param event The current KeyEvent  
 * @param handler The handler, through which remapped inputs will be passed.  
 * @return Whether the input was handled or not.  
 */
public boolean handleKeyEventInput(KeyEvent event, GamepadHandler handler);
```

```java
/**
 * Saves the remapper data inside its own shared preference file 
 * @param context A context object, necessary to fetch SharedPreferences  
 */
public void save(Context context);
```

## RemapperView.Builder
### Constructors
```java
/** @param listener The listener to which the Remapper object is passed after remapping */
public Builder(RemapperView.Listener listener);
```

### Functions
```java
/** @param enabled Enable the remapping of said button. Default is false. */
public Builder remapLeftJoystick(boolean enabled);
public Builder remapRightJoystick(boolean enabled);
public Builder remapLeftJoystickButton(boolean enabled);
public Builder remapRightJoystickButton(boolean enabled);
public Builder remapDpad(boolean enabled);
public Builder remapLeftShoulder(boolean enabled);
public Builder remapRightShoulder(boolean enabled);
public Builder remapLeftTrigger(boolean enabled);
public Builder remapRightTrigger(boolean enabled);
public Builder remapA(boolean enabled);
public Builder remapX(boolean enabled);
public Builder remapY(boolean enabled);
public Builder remapB(boolean enabled);
public Builder remapStart(boolean enabled);
public Builder remapSelect(boolean enabled);
```

```java
/** Set the listener, replacing the one set by the constructor */
public Builder setRemapListener(RemapperView.Listener listener);
```

```java
/**
 * Build and display the remapping dialog with all the parameters set previously
 * @param context A context object referring to the current window  
 */
public void build(Context context);
```

## RemapperManager
Manager class to streamline even more the integration of gamepads  
It auto handles displaying the mapper view and handling events.

Note that the compatibility with a manual integration at the same time is limited

### Constructor
```java
/**
 * @param context A context for SharedPreferences. The Manager attempts to fetch an existing remapper.  
 * @param builder Builder with all the params set in. Note that the listener is going to be overridden.  
 */
public RemapperManager(Context context, RemapperView.Builder builder);
```

### Functions
```java
/**
 * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method 
 * Will automatically ask to remap if no remapper is available 
 * @param event The current MotionEvent  
 * @param handler The handler, through which remapped inputs will be passed.  
 * @return Whether the input was handled or not.  
 */
public boolean handleMotionEventInput(Context context, MotionEvent event, GamepadHandler handler);
```
```java
/**
 * If the event is a valid Gamepad event and a remapper is available, call the GamepadHandler method 
 * Will automatically ask to remap if no remapper is available
 * @param event The current KeyEvent  
 * @param handler The handler, through which remapped inputs will be passed.  
 * @return Whether the input was handled or not.  
 */
public boolean handleKeyEventInput(Context context, KeyEvent event, GamepadHandler handler);
```

## Interface - GamepadHandler
### Functions
```java
/**
 * Function handling all gamepad actions. 
 * @param code
 * Either a keycode, one of: 
 * KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_X, KEYCODE_BUTTON_Y, 
 * KEYCODE_BUTTON_R1, KEYCODE_BUTTON_L1, KEYCODE_BUTTON_START, KEYCODE_BUTTON_SELECT,
 * KEYCODE_BUTTON_THUMBL, KEYCODE_BUTTON_THUMBR
 * Either an axis, one of:
 * AXIS_HAT_X, AXIS_HAT_Y, AXIS_X, AXIS_Y, AXIS_Z, AXIS_RZ, AXIS_RTRIGGER, AXIS_LTRIGGER
 * Note: The code may be different if the gamepad is not fully remapped.
 *
 * @param value For keycodes, 0 for released state, 1 for pressed state.  
 *              For Axis, the value of the axis. Varies between 0/1 or -1/1 depending on the axis.
 */
public void handleGamepadInput(int code, float value);
```

</details>

# License
This project is licensed under the LGPLv3, which allows you to make commercial of software using this library.
Only improvements/modifications done to this very library (and technically the demo project) need to be open sourced !

## Additional credit
Thanks to [thoseawesomeguys](https://thoseawesomeguys.com/prompts/) for the bitmap graphics !



