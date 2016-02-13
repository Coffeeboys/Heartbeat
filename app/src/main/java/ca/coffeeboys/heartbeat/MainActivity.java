package ca.coffeeboys.heartbeat;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import ca.coffeeboys.heartbeat.view.LineGraphView;

public class MainActivity extends AppCompatActivity {
    Firebase db;
    private CameraPreview mPreview;
    private PulseCallback pulseCallback;
    private Camera mCamera;
    private FloatingActionButton fab;
    private LineGraphView mLineGraphView;
    
    private String FIREBASE_ROOT = "Channels";
    private String USERNAME_PREFERENCE = "Username";
    private ValueEventListener dbListener;
    private String currentChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String username = preferences.getString(USERNAME_PREFERENCE, "");
        if (username.equals("")) {
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            dialogBuilder.setTitle("Set Username");
            dialogBuilder.setView(input);
            dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = preferences.edit();
                    String usernameInput = input.getText().toString();
                    editor.putString(USERNAME_PREFERENCE, usernameInput);
                    editor.apply();
                    setupFirebase(getApplicationContext());
                    registerFirebaseListener(usernameInput);
                    pulseCallback = makePulseCallback();
                }
            });
            dialogBuilder.create().show();
        }
        else {
            //SERVER STUFF
            setupFirebase(getApplicationContext());
            registerFirebaseListener(username);
            pulseCallback = makePulseCallback();
        }


        mLineGraphView = (LineGraphView) findViewById(R.id.heart_graph);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBeat(preferences.getString(USERNAME_PREFERENCE, ""));
            }
        });

        fab.setOnTouchListener(new View.OnTouchListener() {
            boolean isPressed = false;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isPressed = true;
                    Log.d("Heartbeat", "Pressed");
                    initCameraPreview();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isPressed = false;
                    Log.d("Heartbeat", "Not Pressed");
                    destroyCameraPreview();
                }
                return false;
            }
        });
    }

    private void destroyCameraPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
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
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_frame);
        preview.addView(mPreview);
    }

    private void registerFirebaseListener(String username) {
        currentChannel = username;
        if (dbListener != null) {
            db.removeEventListener(dbListener);
            db.child(FIREBASE_ROOT).child(username).addValueEventListener(dbListener);
        }
        else {
            dbListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    mVibrator.vibrate(50);
                    animatePulse();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            };
            db.child(FIREBASE_ROOT).child(username).addValueEventListener(dbListener);
        }
    }

    private void setupFirebase(Context applicationContext) {
        Firebase.setAndroidContext(applicationContext);
        db = new Firebase("https://hackentinesheartbeat.firebaseio.com/");
    }

    private PulseCallback makePulseCallback() {
        return new PulseCallback() {
            @Override
            public void onPulse() {
                String username = getUsername();
                sendBeat(username);
                animatePulse();
            }

            @Override
            public void onDataCollected(long pulseValue) {
                mLineGraphView.addPoint(pulseValue, System.currentTimeMillis());
            }
        };
    }

    @NonNull
    private String getUsername() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(USERNAME_PREFERENCE, "");
    }

    private void sendBeat(String username) {
        db.child(FIREBASE_ROOT).child(username).setValue(Calendar.getInstance().getTimeInMillis());
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
        if (id == R.id.action_search) {
            db.child(FIREBASE_ROOT).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String channelName = getCurrentChannel();
                    int channelIndex = 0;
                    HashMap userMap = (HashMap) dataSnapshot.getValue();
                    Set keys = userMap.keySet();
                    final CharSequence[] keyArray = new CharSequence[keys.size()];
                    int i = 0;
                    for (Object key: keys) {
                        if (key instanceof String) {
                            if (key.equals(channelName)) {
                                channelIndex = i;
                            }
                            keyArray[i] = (CharSequence) key;
                        }
                        i++;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.search_icon));
                    builder.setTitle("Select a channel");
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setSingleChoiceItems(keyArray, channelIndex, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String channelName = (String) keyArray[which];
                            registerFirebaseListener(channelName);
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        destroyCameraPreview();
    }

    public String getCurrentChannel() {
        return currentChannel;
    }
}
