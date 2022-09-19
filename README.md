# Controller Remapper for Android
[![](https://jitpack.io/v/Mathias-Boulay/android_gamepad_remapper.svg)](https://jitpack.io/#Mathias-Boulay/android_gamepad_remapper)


An incredibly easy to use, lightweight and widely compatible library to streamline the implementation of gamepads for android applications !

## Why this library ?
According to the  [official android regarding gamepad integration](https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input#button), you have 2 ways of capturing the input for the DPAD and triggers, either via `KeyEvent`  or `MotionEvents`.
But since Android is not standardized enough, the actual buttons/axis reported are swapped, or completely different ones !

This library addresses both issues.

## Ultra quick start
Clone this repository, and fiddle around with the demo project to see how it's made.  Also available as a release !

## Quick start 
### Step 1: Installation
First, add the dependency inside the `build.gradle` file of your app module:

```css
dependencies {
    implementation 'com.github.Mathias-Boulay:android_gamepad_remapper:master-SNAPSHOT'
}
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

### Step 2: Build the remapper
To display the remapping UI to the user, use the RemapperView.Builder object:
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


### Step 3: Make use of the mapped control scheme
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
	}
}
```

# Full documentation
<details>
<summary>Click here to see it in all its glory !</summary>

## Remapper
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
/**  
 * Build and display the remapping dialog with all the parameters set previously
 * @param context A context object referring to the current window  
 */
public void build(Context context);
```

## Interface - GamepadHandler
### Functions
```java
/**  
 * Function handling all gamepad actions. 
 * @param code Either a keycode (Eg. KEYBODE_BUTTON_A), either an axis (Eg. AXIS_HAT_X)  
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



