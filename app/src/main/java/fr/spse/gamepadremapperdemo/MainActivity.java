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
                /*
                        .remapDpad(true)
                        .remapLeftJoystick(true)
                        .remapRightJoystick(true)
                        .remapStart(true)
                        .remapSelect(true)
                        .remapLeftShoulder(true)
                        .remapRightShoulder(true)

                 */
                        .build( findViewById(R.id.main_layout), null, 0, 0);

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Toast.makeText(this, KeyEvent.keyCodeToString(keyCode) + "->" + KeyEvent.keyCodeToString(remapper.getRemappedSource(event)), Toast.LENGTH_SHORT).show();
        return true; //return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        System.out.println( MotionEvent.axisToString(RemapperView.findTriggeredAxis(event)) + "->");

        return false; //return super.onGenericMotionEvent(event);
    }
}