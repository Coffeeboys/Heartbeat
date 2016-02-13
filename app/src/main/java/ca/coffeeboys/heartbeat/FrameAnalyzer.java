package ca.coffeeboys.heartbeat;

import android.hardware.Camera;
import android.util.Log;

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
    private PulseCallback pulseCallback;


    public FrameAnalyzer(PulseCallback callback) {
        prevAverage = 0;
        nextAverage = 0;
        beatState = false;
        movingAverageArray = new long[averageArraySize];
        movingAverageIndex = 0;
        pulseCallback = callback;
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

        //Calculate average of new image
        long byteTotal = 0;
        for (byte aData : data) {
            byteTotal += (long) aData;
        }
        nextAverage = byteTotal/data.length;


        boolean newBeatState;
        if (nextAverage < movingAverage) {
            pulseCallback.onPulse();
            newBeatState = true;
        }
        else {
            newBeatState = false;
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
