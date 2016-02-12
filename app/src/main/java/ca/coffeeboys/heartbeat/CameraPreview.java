package ca.coffeeboys.heartbeat;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by Aarjente! on 2016-02-12.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
//    private byte[] pixelData;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
//        int lemma = ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat());
//        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
//        pixelData = new byte[(int)(previewSize.height * previewSize.width * lemma)];
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
//            mCamera.addCallbackBuffer(pixelData);
        }
        catch (IOException e) {
            //
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        if (mSurfaceHolder.getSurface() == null) {
//            return;
//        }
//        try {
//            mCamera.stopPreview();
//        }
//        catch (Exception e) {
//            //
//        }
//
//        try {
//            mCamera.setPreviewDisplay(mSurfaceHolder);
//            mCamera.startPreview();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //SURFACE DESTROYED
    }
}
