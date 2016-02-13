package ca.coffeeboys.heartbeat;

import android.hardware.Camera;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import ca.coffeeboys.heartbeat.util.YUV420Decoder;

/**
 * Created by Aarjente! on 2016-02-12.
 */
public class FrameAnalyzer implements Camera.PreviewCallback {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
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
    //volatile so that thread caching issues won't occur. This may not be necessary with booleans but just in case
    private volatile boolean isEnabled = false;



    public FrameAnalyzer(PulseCallback callback) {
        prevAverage = 0;
        nextAverage = 0;
        beatState = false;
        movingAverageArray = new long[averageArraySize];
        movingAverageIndex = 0;
        pulseCallback = callback;
        setLastPulseTime(System.currentTimeMillis());
    }

    private synchronized long getLastPulseTime() {
        return lastPulseTime;
    }

    private synchronized  void setLastPulseTime(long lastPulseTime) {
        this.lastPulseTime = lastPulseTime;
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!isEnabled) {
                    return;
                }
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
                /*long byteTotal = 0;
                for (byte aData : data) {
                    byteTotal += (long) aData;
                }
                nextAverage = byteTotal/data.length;*/
                if (data == null) {
                    Log.e(this.getClass().getCanonicalName(), "byte data is null");
                    return;
                }
                nextAverage = YUV420Decoder.decodeYUV420SPtoRedAvg(data, previewWidth, previewHeight );


                boolean newBeatState = false;
                if (nextAverage < movingAverage) {
                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    if (currentTime - getLastPulseTime() > pulseDelay) {
                        pulseCallback.onPulse();
                        newBeatState = true;
                        setLastPulseTime(currentTime);
                    }
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
        });
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
