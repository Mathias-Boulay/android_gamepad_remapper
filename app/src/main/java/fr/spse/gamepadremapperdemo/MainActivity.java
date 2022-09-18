package fr.spse.gamepadremapperdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import fr.spse.gamepad_remapper.Remapper;
import fr.spse.gamepad_remapper.RemapperView;

public class MainActivity extends AppCompatActivity {

    private Remapper remapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new RemapperView.Builder(new RemapperView.Listener() {
                    @Override
                    public void onRemapDone(Remapper remapper) {
                        Toast.makeText(getBaseContext(), "Mapping done !", Toast.LENGTH_LONG).show();
                        MainActivity.this.remapper = remapper;
                    }
                })
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
                        .remapRightTrigger(true)


                        .build( findViewById(R.id.main_layout), null, 0, 0);

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        System.out.println(KeyEvent.keyCodeToString(keyCode) + "->" + KeyEvent.keyCodeToString(remapper.getRemappedSource(event)));
        return true; //return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if(Remapper.findTriggeredAxis(event) == Remapper.AXIS_NONE) return false;

        System.out.println( MotionEvent.axisToString(Remapper.findTriggeredAxis(event)) + "->");

        return true; //return super.onGenericMotionEvent(event);
    }
}