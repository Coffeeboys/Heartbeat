package ca.coffeeboys.heartbeat;

import android.hardware.Camera;
import android.util.Log;

import java.util.Calendar;

import ca.coffeeboys.heartbeat.util.YUV420Decoder;

/**
 * Created by Aarjente! on 2016-02-12.
 */
public class FrameAnalyzer implements Camera.PreviewCallback {

    private long prevAverage;
    private long nextAverage;
    private long movingAverage;
    private boolean beatState;
    private long[] movingAverageArray;
    private int movingAverageIndex;
    private int averageArraySize = 3;
    private long lastPulseTime;
    private int pulseDelay = 200;
    private PulseCallback pulseCallback;
    private int nonRedCount = 100;
    private int redThreshold = 180;
    private boolean redDetected = true;



    public FrameAnalyzer(PulseCallback callback) {
        prevAverage = 0;
        nextAverage = 0;
        beatState = false;
        movingAverageArray = new long[averageArraySize];
        movingAverageIndex = 0;
        pulseCallback = callback;
        lastPulseTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        Camera.Size size = camera.getParameters().getPreviewSize();
        int previewWidth = size.width;
        int previewHeight = size.height;



        //Calculate average of last set of beats
        long currentArrayAverage = 0;
        int currentArrayCount = 0;
        for (Long arrayAverage: movingAverageArray) {
            if (arrayAverage != 0) {
                currentArrayAverage += arrayAverage;
                currentArrayCount++;
            }
        }
        if (currentArrayCount > 0 ){
            movingAverage = currentArrayAverage/currentArrayCount;

        }

        nextAverage = YUV420Decoder.decodeYUV420SPtoRedAvg(data.clone(), previewWidth, previewHeight );
        Log.d("Heartbeat", "" + (Math.abs(nextAverage - movingAverage)));
        if (nextAverage < redThreshold) {
            if (redDetected) {
                pulseCallback.onPulseNotDetected();
                redDetected = false;
            }
        }
        else {
            redDetected = true;
        }
        /*if (Math.abs(nextAverage - movingAverage) < 10) {
            pulseCallback.onDataCollected(Math.abs(nextAverage));
        }*/

        boolean newBeatState = false;
        if (nextAverage < movingAverage) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - lastPulseTime > pulseDelay) {
                pulseCallback.onPulse();
                pulseCallback.onDataCollected((1));
                newBeatState = true;
                lastPulseTime = currentTime;
            }
        }
        else {
            newBeatState = false;
            pulseCallback.onDataCollected(0);
        }

        movingAverageArray[movingAverageIndex] = nextAverage;
        movingAverageIndex = (movingAverageIndex + 1) % averageArraySize;

        beatState = newBeatState;




        /*long byteTotal = 0;
        for (byte aData : data) {
            byteTotal += (long) (aData * aData);
        }
        long byteTotalAverage = byteTotal/data.length;
        movingAverageArray[movingAverageIndex] = byteTotalAverage;
        movingAverageIndex = (movingAverageIndex + 1) % averageArraySize;

        nextAverage = 0;
        for (Long currentAverage : movingAverageArray) {
            nextAverage += currentAverage;
        }
        nextAverage = nextAverage / averageArraySize;

        if ((Math.abs(nextAverage - prevAverage) > 37) && (Math.abs(nextAverage - prevAverage) < 200))  {
            Log.d("FrameAnalyzer", "" + nextAverage);
            pulseCallback.onPulse();
        }
        prevAverage = nextAverage;*/
    }
}
