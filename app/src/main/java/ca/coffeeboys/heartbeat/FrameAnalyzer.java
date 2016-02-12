package ca.coffeeboys.heartbeat;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by Aarjente! on 2016-02-12.
 */
public class FrameAnalyzer implements Camera.PreviewCallback {
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("FrameAnalyzer", "Hi");
    }
}
