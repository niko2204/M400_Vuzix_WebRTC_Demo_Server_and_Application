package owt.sample.p2p;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;

public class CallFragment extends Fragment implements SurfaceHolder.Callback {

    private static final String TAG = "VuzixCallFra";
    private SurfaceViewRenderer fullRenderer, smallRenderer;
    private CallFragmentListener mListener;
    private float dX, dY;
    private boolean isPublishing = false;
    private SurfaceView ov;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SurfaceHolder sh;
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == R.id.small_renderer) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        v.animate()
                                .x(event.getRawX() + dX >= event.getRawY() + dY ? event.getRawX()
                                        + dX : 0)
                                .y(event.getRawX() + dX >= event.getRawY() + dY ? 0
                                        : event.getRawY() + dY)
                                .setDuration(10)
                                .start();
                        break;
                }
            }
            return true;
        }
    };

    public CallFragment() {
    }

    public void updateDisplay(ArrayList<BaseObject> drawObjectList){

        Canvas canvas = sh.lockCanvas();
        int x = canvas.getWidth();
        int y = canvas.getHeight();
        // clear screen and render objects

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        for(int c=0; c<drawObjectList.size(); c++){
            if(drawObjectList.get(c).hasSourceDimensions()){
                // set width and height of canvas
                Log.d("updateDisplay","Canvas Width: " + x + " Height: " + y + "" );
                drawObjectList.get(c).setCanvasWidth(x);
                drawObjectList.get(c).setCanvasHeight(y);
            }
            drawObjectList.get(c).drawObject(canvas,paint);
        }

        sh.unlockCanvasAndPost(canvas);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_call, container, false);


        ov = mView.findViewById(R.id.overlaySurface);
        ov.setZOrderMediaOverlay(true);
        ov.setBackgroundColor(Color.TRANSPARENT);

        sh = ov.getHolder();
        sh.setFormat(PixelFormat.TRANSPARENT);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        sh.addCallback(this);

        fullRenderer = mView.findViewById(R.id.full_renderer);
        fullRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        fullRenderer.setEnableHardwareScaler(true);
        smallRenderer = mView.findViewById(R.id.small_renderer);
        smallRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        smallRenderer.setOnTouchListener(touchListener);
        smallRenderer.setEnableHardwareScaler(true);
        smallRenderer.setZOrderMediaOverlay(true);

        mListener.onReady(fullRenderer, smallRenderer);
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (CallFragmentListener) context;
    }

    @Override
    public void onDetach() {
        fullRenderer.release();
        fullRenderer = null;
        smallRenderer.release();
        smallRenderer = null;
        super.onDetach();
    }


    void onPublished(final boolean succeed) {
        getActivity().runOnUiThread(() -> {
            isPublishing = succeed;
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public interface CallFragmentListener {
        void onReady(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);

        void onPublishRequest();

        void onUnpublishRequest(boolean stop);
    }
}
