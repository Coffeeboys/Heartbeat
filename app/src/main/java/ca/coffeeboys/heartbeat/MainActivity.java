package ca.coffeeboys.heartbeat;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    Firebase db;
    private CameraPreview mPreview;
    private PulseCallback pulseCallback;
    private Camera mCamera;
    private FloatingActionButton fab;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //SERVER STUFF
        setupFirebase(getApplicationContext());
        registerFirebaseListener(getWindow().getDecorView().getRootView());
        pulseCallback = makePulseCallback();

        //CAMERA STUFF
//        initCameraPreview(getCameraInstance());


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBeat();
//                Snackbar.make(view, "Send data", Snackbar.LENGTH_LONG).show();
//                Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//                mVibrator.vibrate(100);
            }
        });
        fab.setOnTouchListener(new View.OnTouchListener() {
            boolean isPressed = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isPressed = true;
                    Log.d("Heartbeat", "Pressed");
                    mCamera = getCameraInstance();
                    initCameraPreview();
                }
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isPressed = false;
                    Log.d("Heartbeat", "Not Pressed");
                    destroyCameraPreview();
                }
                return false;
            }
        });
    }

    private void destroyCameraPreview() {
        mCamera.stopPreview();
        mPreview.getHolder().removeCallback(mPreview);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
        mCamera.release();
        mCamera = null;

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_frame);
        preview.removeView(mPreview);
    }

    public Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
           // parameters.setPreviewFormat(ImageFormat.RGB_565);
            camera.setParameters(parameters);
            camera.setPreviewCallback(new FrameAnalyzer(pulseCallback));
            camera.setDisplayOrientation(90);
        }
        catch (Exception e) {
            //handle camera errors for non-hackathon porpoises
        }
        return camera;
    }

    private void initCameraPreview() {
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_frame);
        preview.addView(mPreview);
    }

    private void registerFirebaseListener(final View view) {
        db.child("Beat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Snackbar.make(view, "Received beat", Snackbar.LENGTH_LONG).show();
                Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                mVibrator.vibrate(100);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void setupFirebase(Context applicationContext) {
        Firebase.setAndroidContext(applicationContext);
        db = new Firebase("https://hackentinesheartbeat.firebaseio.com/");
    }

    private PulseCallback makePulseCallback() {
        return new PulseCallback() {
            @Override
            public void onPulse() {
                sendBeat();
                animatePulse();
            }
        };
    }

    private void sendBeat() {
        db.child("Beat").setValue(Calendar.getInstance().getTimeInMillis());
    }
    
    private void animatePulse() {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(MainActivity.this,
                R.animator.pulse);
        set.setTarget(fab);
        set.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            destroyCameraPreview();
        }
    }
}
