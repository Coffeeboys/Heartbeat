package ca.coffeeboys.heartbeat.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Trevor on 2/12/2016.
 */
public class  LineGraphView extends View {
    Path mPath;
    Long prevTime = null;
    Paint mPaint = new Paint();

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPath = new Path();
        mPaint.setColor(getResources().getColor(android.R.color.black));
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(20);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    //beautiful hardcoded ranges
    float maxTime = 2000;
    float maxY = 30;

    public void addPoint(long y, long currTime) {
        if (prevTime != null) {
            long difference = currTime - prevTime;
            float offset = difference / (maxTime / getWidth()); //put the offset in the range of the screen pixels
            mPath.offset(-offset, 0);
        } else {
            //move the 'pen' to the center of the screen without actually drawing a line for the first point
            mPath.moveTo(getCenterXCoord(), y);
        }

        mPath.lineTo(getCenterXCoord(), Math.abs(y) / (maxY / getHeight()));

        prevTime = currTime;

        //draw lines
        invalidate();
    }

    private int getCenterXCoord() {
        return getWidth()/2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        float x = event.getX();
//        float y = event.getY();
//        switch(event.getAction()) {
//            case MotionEvent.ACTION_DOWN :
//                mPath.moveTo(x, y);
//                break;
//            case MotionEvent.ACTION_MOVE :
//                mPath.lineTo(x, y);
//                break;
//        }
//        invalidate();
//        return true;
        return super.onTouchEvent(event);
    }
}
