package ca.coffeeboys.heartbeat;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback{
    Firebase db;
    private CameraPreview mPreview;
    private PulseCallback pulseCallback;
    private Camera mCamera;
    private FloatingActionButton fab;
    
    private String FIREBASE_ROOT = "Channels";
    private String CHANNEL_PREFERENCE = "Channel";
    private ValueEventListener dbListener;
    private boolean lineChartInitialized;
    private float[] lineChartValues;
    private int lineChartValuesIndex;
    private int lineChartValuesSize;
    private float minValue;
    private float maxDiff;

    private MediaPlayer soundPlayer;
    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
//            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
//            finish();
//            return;
            //TODO: do some meaningful error handling
        }
        nfcAdapter.setNdefPushMessageCallback(this, this);


        soundPlayer = MediaPlayer.create(this, R.raw.heartbass);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String username = preferences.getString(CHANNEL_PREFERENCE, "");
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
                    editor.putString(CHANNEL_PREFERENCE, usernameInput);
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

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBeat(getCurrentChannel());
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
                    initCameraPreview();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isPressed = false;
                    Log.d("Heartbeat", "Not Pressed");
                    destroyCameraPreview();
                }
                return false;
            }
        });

        lineChartInitialized = false;
        lineChartValuesSize = 100;
        lineChartValues = new float[lineChartValuesSize];
        lineChartValuesIndex = 0;
        minValue = 0;
        maxDiff = 1;

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

//            LineChartView lineChart = (LineChartView) findViewById(R.id.pulse_chart);
//            lineChartValues.beginAt(5);
//            lineChartValues.setThickness(3);
//            lineChartValues.setSmooth(true);
//            lineChart.addData(lineChartValues);
//            lineChart.setYLabels(AxisController.LabelPosition.NONE);
//            lineChart.setYAxis(false);
//            lineChart.setXAxis(false);
//            lineChart.show();
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

    private void registerFirebaseListener(final String channel) {
        if (dbListener != null) {
            db.child(FIREBASE_ROOT).child(getCurrentChannel()).removeEventListener(dbListener);
        }
        setCurrentChannel(channel);
        dbListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    sendBeat(channel);
                }
                else {
                    Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    mVibrator.vibrate(100);
                    animatePulse();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
        db.child(FIREBASE_ROOT).child(channel).addValueEventListener(dbListener);
    }

    private void setupFirebase(Context applicationContext) {
        Firebase.setAndroidContext(applicationContext);
        db = new Firebase("https://hackentinesheartbeat.firebaseio.com/");
    }

    private PulseCallback makePulseCallback() {
        return new PulseCallback() {
            @Override
            public void onDataCollected(long pulseValue) {
//                lineChartValues.addPoint(new Point("", pulseValue));
                updateGraph(pulseValue);
            }

            @Override
            public void onPulse() {
                soundPlayer.start();
                sendBeat(getCurrentChannel());
                animatePulse();
            }
        };
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

    private void updateGraph(float pulseValue) {
        final LineChartView lineChart = (LineChartView) findViewById(R.id.pulse_chart);

        if (lineChartInitialized) {
            lineChartValuesIndex = lineChartValuesSize;
            if (lineChartValuesIndex < lineChartValuesSize) {
//                lineChartValues[lineChartValuesIndex] = (pulseValue-minValue)/maxDiff;
                lineChartValues[lineChartValuesIndex] = pulseValue;
                lineChartValuesIndex++;
//                if (lineChartValuesIndex == 10) {
//                    float[] sortedArray = Arrays.copyOfRange(lineChartValues, 0 ,10);
//                    Arrays.sort(sortedArray);
//                    for (float currVal : sortedArray) {
//                        if (currVal > 0) {
//                            minValue = currVal;
//                            break;
//                        }
//                    }
//                    for (int i = 0; i < 10; ++i) {
//                        lineChartValues[i] -= minValue;
//                    }
//                    maxDiff = lineChartValues[9];
////                    maxDiff*=1.3;
//                    for (int i = 0; i < 10; ++i) {
//                        lineChartValues[i] = lineChartValues[i]/maxDiff;
//                    }
//                }
            }
            else {
                for(int i = 1; i < lineChartValuesSize; i++) {
                    lineChartValues[i - 1] = lineChartValues[i];
                }
//                lineChartValues[lineChartValuesIndex - 1] = (pulseValue-minValue)/maxDiff;
                lineChartValues[lineChartValuesIndex - 1] = pulseValue;
                lineChart.updateValues(0, lineChartValues);
                lineChart.show();
            }
        }
        else {
            String[] stringSet = new String[lineChartValuesSize];
            Arrays.fill(stringSet, "");
            LineSet newSet = new LineSet(stringSet, lineChartValues);
    //            newSet.addPoint(new Point("", pulseValue));
            newSet.setThickness(3);
            newSet.setSmooth(true);
            lineChart.setYLabels(AxisController.LabelPosition.NONE);
            lineChart.setYAxis(false);
            lineChart.setXAxis(false);
            lineChart.addData(newSet);
            lineChart.show();
            lineChartInitialized = true;
        }
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
        else if (id == R.id.action_create) {
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            dialogBuilder.setTitle("Create Channel");
            dialogBuilder.setView(input);
            dialogBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String channelName = input.getText().toString();
                    setCurrentChannel(channelName);
                    setupFirebase(getApplicationContext());
                    registerFirebaseListener(channelName);
                }
            });
            dialogBuilder.create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        destroyCameraPreview();
    }

    public String getCurrentChannel() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(CHANNEL_PREFERENCE, "");
    }

    public void setCurrentChannel(String currentChannel) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CHANNEL_PREFERENCE, currentChannel);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Snackbar.make(fab, "NDEF Discovered", Snackbar.LENGTH_LONG).show();
            receiveNfcIntent(getIntent());
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        Snackbar.make(fab, "Making NDef message", Snackbar.LENGTH_LONG).show();
        return new NdefMessage(
                new NdefRecord[]{ NdefRecord.createMime(
                        "text/plain", currentChannel.getBytes())
//                        NdefRecord.createApplicationRecord("ca.coffeeboys.heartbeat")
                });
    }

    void receiveNfcIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
//        textView.setText(new String(msg.getRecords()[0].getPayload()));
        currentChannel = new String(msg.getRecords()[0].getPayload());
        registerFirebaseListener(currentChannel);
        Snackbar.make(fab, "registered to " + currentChannel + " channel", Snackbar.LENGTH_LONG).show();
    }
}
