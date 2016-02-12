package ca.coffeeboys.heartbeat;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by Aarjente! on 2016-02-12.
 */
public class FrameAnalyzer implements Camera.PreviewCallback {

    private long prevAverage;
    private long nextAverage;
    private long[] movingAverageArray;
    private int movingAverageIndex;
    private int averageArraySize = 10;


    public FrameAnalyzer() {
        prevAverage = 0;
        nextAverage = 0;
        movingAverageArray = new long[averageArraySize];
        movingAverageIndex = 0;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long byteTotal = 0;
        for (byte aData : data) {
            byteTotal += (long) (aData * aData);
        }
        long byteTotalAverage = byteTotal/data.length;
        movingAverageArray[movingAverageIndex] = byteTotalAverage;
        movingAverageIndex = (movingAverageIndex + 1) % averageArraySize;

        nextAverage = 0;
        for (Long currentAverage: movingAverageArray) {
            nextAverage += currentAverage;
        }
        nextAverage = nextAverage/averageArraySize;

        if ((Math.abs(nextAverage- prevAverage) > 40) && (Math.abs(nextAverage- prevAverage) < 200))  {
            Log.d("FrameAnalyzer", "" + nextAverage);
        }
        prevAverage = nextAverage;
    }
}
