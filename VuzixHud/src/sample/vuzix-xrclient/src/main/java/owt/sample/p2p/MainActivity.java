package owt.sample.p2p;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.H265;
import static owt.base.MediaCodecs.VideoCodec.VP8;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vuzix.sdk.barcode.ScanResult;
import com.vuzix.sdk.barcode.ScannerIntent;
import android.content.ActivityNotFoundException;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.RTCStatsReport;
import org.webrtc.SurfaceViewRenderer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.ContextInitialization;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.base.VideoEncodingParameters;
import owt.p2p.P2PClient;
import owt.p2p.P2PClientConfiguration;
import owt.p2p.Publication;
import owt.p2p.RemoteStream;
import owt.sample.utils.OwtVideoCapturer;

public class MainActivity extends AppCompatActivity implements SensorEventListener,
        CallFragment.CallFragmentListener, P2PClient.P2PClientObserver, VuzixChannelObserver {

    private static final String TAG = "VuzixMainAct";
    private static final int OWT_REQUEST_CODE = 100;
    private static final int STATS_INTERVAL_MS = 10000;
    private static final int REQUEST_CODE_SCAN = 49374;

    private final static int LOGIN = 1;
    private final static int LOGOUT = 2;
    private final static int INVITE = 3;
    private final static int STOP = 4;
    private final static int PUBLISH = 5;
    private final static int UNPUBLISH = 6;
    private final static int SWITCH_CAMERA = 7;
    private final static int SEND_DATA = 8;
    private final static int MSG_STOP_SHARESCREEN = 9;
    private final static int MSG_START_SHARESCREEN = 10;
    private final static int INVITEME = 11;
    private final static int SEND_IMU = 12;
    private final static int SEND_BAT = 13;
    private final static int SEND_STEPS = 14;
    private final static int SEND_AMB = 15;
    private final static int DISCONNECT = 16;

    static Activity myActivity = null;
    EglBase rootEglBase;

    private CallFragment callFragment;
    private TextView messageText;
    private SurfaceViewRenderer localRenderer, remoteRenderer;
    private P2PClient p2PClient;
    private Publication publication;
    private String peerId;
    private boolean inCalling = false;
    private SocketSignalingChannel socketChannel = null;

    private LocalStream localStream;
    private RemoteStream remoteStream;
    private boolean remoteStreamEnded = false;
    private OwtVideoCapturer capturer;

    private Timer statsTimer;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean enableLocalStream = false;

    private EditText serverText, myIdText, peerIdText;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private Sensor mStepSensor;
    private Sensor mTempSensor;
    private Sensor mLightSensor;

    private final float[] mRotationMatrix = new float[16];
    private final int[] mBattery = new int[3];
    private final int[] mSteps = new int[1];
    private final float[] mAmbientLight = new float[1];
    private IntentFilter iFilter;

    private HandlerThread peerThread;
    private PeerHandler peerHandler;
    private Message message;

    private ArrayList<BaseObject> drawObjectList;

    public void onSuperBackPressed(){
        Log.d(TAG, "OnSuperBackPressed()");
        finish();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (tasks != null && tasks.size() > 0) {
                tasks.get(0).setExcludeFromRecents(true);
            }
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"Back Button Pressed");
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Client App")
                .setMessage("Press Back to exit or select to cancel?")
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_BACK:

                                Log.d(TAG, "Finish called");

                                if(p2PClient != null) {
                                    message = peerHandler.obtainMessage();
                                    message.what = DISCONNECT;
                                    message.sendToTarget();

                                    executor.execute(() -> p2PClient.disconnect());
                                }

                                dialog.dismiss();
                                ((MainActivity)myActivity).onSuperBackPressed();
                                return true;

                            default:
                                return false;
                        }
                    }
                })
                .setPositiveButton("Exit", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        ((MainActivity)myActivity).onSuperBackPressed();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        myActivity = this;

        serverText = new EditText(this);
        myIdText = new EditText(this);
        peerIdText = new EditText(this);
        messageText = findViewById(R.id.mainWindowMessage);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        drawObjectList = new ArrayList<>();

        iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        peerThread = new HandlerThread("PeerThread");
        peerThread.start();
        peerHandler = new PeerHandler(peerThread.getLooper());

        initP2PClient();

        Log.d(TAG, "QR Connect Auto");
        Intent scannerIntent = new Intent(ScannerIntent.ACTION);
        try {
            // The Vuzix M300 has a built-in Barcode Scanner app that is registered for this intent.
            startActivityForResult(scannerIntent, REQUEST_CODE_SCAN);
        } catch (ActivityNotFoundException activityNotFound) {
            Toast.makeText(this, R.string.only_on_mseries, Toast.LENGTH_LONG).show();
        }
    }

    private void initP2PClient() {
        rootEglBase = EglBase.create();

        try{
            ContextInitialization.create()
                    .setApplicationContext(this)
                    .setVideoHardwareAccelerationOptions(
                            rootEglBase.getEglBaseContext(),
                            rootEglBase.getEglBaseContext())
                    .initialize();

            VideoEncodingParameters h264 = new VideoEncodingParameters(H264);
            VideoEncodingParameters h265 = new VideoEncodingParameters(H265);
            VideoEncodingParameters vp8 = new VideoEncodingParameters(VP8);
            P2PClientConfiguration configuration = P2PClientConfiguration.builder()
                    .addVideoParameters(h264)
                    .addVideoParameters(vp8)
                    .addVideoParameters(h265)
                    .build();
            socketChannel = new SocketSignalingChannel();
            socketChannel.setVuzixChannelObserver(this);
            p2PClient = new P2PClient(configuration, socketChannel);
            p2PClient.addObserver(this);
        }
        catch (RuntimeException ex)
        {
            Log.e(TAG, "Could not initP2PClient. ", ex);
        }
    }

    private void switchFragment(final Fragment fragment, final boolean showMessage) {
        runOnUiThread( () -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
            if (showMessage) {
                messageText.setVisibility(TextView.VISIBLE);
            } else {
                messageText.setVisibility(TextView.GONE);
            }
        });
    }

    private void requestPermission() {
        runOnUiThread( () -> {
            String[] permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO};

            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        permission) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            permissions,
                            OWT_REQUEST_CODE);
                    Log.d(TAG, "Need permission");
                    return;
                }
                onConnectSucceed();
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == OWT_REQUEST_CODE
                && grantResults.length == 2
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED) {
            onConnectSucceed();
        }
    }

    private void onConnectSucceed() {
        runOnUiThread( () -> {
            Log.d(TAG, "onConnectSucceed");
            messageText.setVisibility(TextView.VISIBLE);
            messageText.setText(R.string.connected);
        });
    }

    private void onConnectFailed(final String messageIn) {
        runOnUiThread( () -> {
            String reason = messageIn;
            Log.e(TAG, "Connect failed: " + reason);
            if ((reason == null) || reason.length() == 0) {
                reason = getString(R.string.failed_connect);
            }
            Toast.makeText(this,reason, Toast.LENGTH_SHORT).show();
            messageText.setText(R.string.failed_connect);
            finish();
        });
    }

    @Override
    public void onServerDisconnected() {
        runOnUiThread( ()-> {
            messageText.setText(R.string.disconnected);
            Log.d(TAG, "onServerDisconnect");
        });
    }

    @Override
    public void onStreamAdded(final RemoteStream remoteStream) {
        this.remoteStream = remoteStream;
        remoteStream.addObserver(new RemoteStream.StreamObserver() {
            @Override
            public void onEnded() {
                remoteStreamEnded = true;
                remoteRenderer.clearImage();
            }

            @Override
            public void onUpdated() {
                // ignored in p2p.
            }
        });
        executor.execute(() -> {
            if (remoteRenderer != null) {
                remoteStream.attach(remoteRenderer);
            }
        });
    }

    @Override
    public void onDataReceived(String peerId, String message) {
        Log.d(TAG, "onDataReceived from: " + peerId + " msg:" + message);

    }

    private void connectRequest(final String server, final String myId) {
        messageText.setText(R.string.connecting);
        executor.execute(() -> {
            JSONObject loginObj = new JSONObject();
            try {
                loginObj.put("host", server);
                loginObj.put("token", myId);
            } catch (JSONException e) {
                Log.e(TAG, "Could not make json request. ", e);
            }
            p2PClient.addAllowedRemotePeer(myId);
            p2PClient.connect(loginObj.toString(), new ActionCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "P2P connected");
                    requestPermission();
                }

                @Override
                public void onFailure(OwtError error) {
                    onConnectFailed(error.errorMessage);
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    ScanResult result = data.getParcelableExtra(ScannerIntent.RESULT_EXTRA_SCAN_RESULT);
                    try {
                        JSONObject p2pConnectObject = new JSONObject(result.getText());
                        final String connectURL = "http://" + p2pConnectObject.getString("ipaddr") + ":" + p2pConnectObject.getInt("port");
                        final String myId = p2pConnectObject.getString("devid");
                        Log.d(TAG, "Scanned QR code. Connecting as: " + myId + " to: " + connectURL);
                        connectRequest(connectURL, myId);
                    } catch (JSONException e) {
                        Log.e(TAG, "QR Connect Failed: " + e.getMessage());
                        onConnectFailed(getString(R.string.bad_qr_scanned));
                    }

                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onDisconnectRequest() {
        executor.execute(() -> p2PClient.disconnect());
    }

    private void callRequest(final String peerId) {
        inCalling = true;
        this.peerId = peerId;
        executor.execute(() -> {
            p2PClient.addAllowedRemotePeer(peerId);
            if (callFragment == null) {
                callFragment = new CallFragment();
            }
            switchFragment(callFragment, false);

            message = peerHandler.obtainMessage();
            message.what = INVITE;
            message.sendToTarget();
        });
    }

    @Override
    public void onReady(final SurfaceViewRenderer localRenderer,
            final SurfaceViewRenderer remoteRenderer) {
        this.localRenderer = localRenderer;
        this.remoteRenderer = remoteRenderer;
        localRenderer.init(rootEglBase.getEglBaseContext(), null);
        remoteRenderer.init(rootEglBase.getEglBaseContext(), null);

        executor.execute(() -> {
            if (capturer == null) {
                capturer = OwtVideoCapturer.create(1280, 720, 30, true, true);
                localStream = new LocalStream(capturer,
                        new MediaConstraints.AudioTrackConstraints());
            }
            localStream.attach(localRenderer);
            if (remoteStream != null && !remoteStreamEnded) {
                remoteStream.attach(remoteRenderer);
            }
        });
    }

    @Override
    public void onPublishRequest() {
        if (!enableLocalStream) {
            localStream.enableAudio();
            localStream.enableVideo();
            enableLocalStream = true;
        }

        executor.execute(
                () -> p2PClient.publish(peerId, localStream, new ActionCallback<Publication>() {
                    @Override
                    public void onSuccess(Publication result) {
                        inCalling = true;
                        publication = result;
                        callFragment.onPublished(true);

                        if (statsTimer != null) {
                            statsTimer.cancel();
                            statsTimer = null;
                        }
                        statsTimer = new Timer();
                        statsTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                getStats();
                            }
                        }, 0, STATS_INTERVAL_MS);
                    }

                    @Override
                    public void onFailure(OwtError error) {
                        callFragment.onPublished(false);

                        if (error.errorMessage.equals("Duplicated stream.")) {
                            //this mean you have published, so change the button to unpublish
                            callFragment.onPublished(true);
                        }
                    }
                }));
    }


    private void fnDrawHandler(JSONObject obj){
        String sdat;
        JSONObject data;
        String typ;
        String cl;
        int c;
        float x1;
        float y1;
        float x2;
        float y2;
        float r1;

        try {
            String sType = obj.getString("type");
            switch(sType){
                case "clear":
                    Log.d(TAG,"fnDrawHandler:Clear Screen");
                    drawObjectList.clear();
                    break;
                case "line":
                    Log.d(TAG,"fnDrawHandler:Draw Line");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    x2 = BigDecimal.valueOf(data.getDouble("x2")).floatValue();
                    y2 = BigDecimal.valueOf(data.getDouble("y2")).floatValue();
                    LineObject lo = new LineObject(x1,y1,x2,y2,c);
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        lo.setSrcWidth(data.getInt("w"));
                        lo.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(lo);
                    break;
                case "circle":
                    Log.d(TAG,"fnDrawHandler:Draw Circle");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    r1 = BigDecimal.valueOf(data.getDouble("r1")).floatValue();
                    CircleObject co = new CircleObject(x1,y1,r1,c);
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        co.setSrcWidth(data.getInt("w"));
                        co.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(co);
                    break;
                case "square":
                    Log.d(TAG,"fnDrawHandler:Draw Square");
                    sdat = obj.getString("data");
                    data = new JSONObject(sdat);
                    typ = data.getString("p");
                    cl = data.getString("c");
                    c = Color.parseColor(cl);
                    x1 = BigDecimal.valueOf(data.getDouble("x1")).floatValue();
                    y1 = BigDecimal.valueOf(data.getDouble("y1")).floatValue();
                    x2 = BigDecimal.valueOf(data.getDouble("x2")).floatValue();
                    y2 = BigDecimal.valueOf(data.getDouble("y2")).floatValue();
                    SquareObject so = new SquareObject (y1,x1,y2,x2,  Color.argb(125, 255, 0, 0));
                    // check if translation is needed
                    if(!data.isNull("w") && !data.isNull("h")){
                        so.setSrcWidth(data.getInt("w"));
                        so.setSrcHeight(data.getInt("h"));
                    }
                    drawObjectList.add(so);

                    break;
                default:
                    Log.d(TAG,"fnDrawHandler: Unkown Draw Command");
                    break;
            }


            runOnUiThread( () -> {
                if(callFragment != null) {
                    callFragment.updateDisplay(drawObjectList);
                }
            });

        }catch(Exception e){
            Log.e(TAG, "fnDrawHandler Error: " + e.getLocalizedMessage());
        }
    }

    private void getStats() {
        if (publication != null) {
            publication.getStats(new ActionCallback<RTCStatsReport>() {
                @Override
                public void onSuccess(RTCStatsReport result) {

                }

                @Override
                public void onFailure(OwtError error) {

                }
            });
        }
    }

    @Override
    public void onUnpublishRequest(boolean back2main) {
        if (enableLocalStream) {
            localStream.disableAudio();
            localStream.disableVideo();
            enableLocalStream = false;
        }

        if (back2main) {
            inCalling = false;

            if (publication != null) {
                publication.stop();
                publication = null;
            }

            if (capturer != null) {
                capturer.stopCapture();
                capturer.dispose();
                capturer = null;
            }
            if (localStream != null) {
                localStream.dispose();
                localStream = null;
            }
            executor.execute(() -> p2PClient.stop(peerId));
        }
    }

    @Override
    public void onVuzixMessage(String from, String msg) {
        Log.i(TAG, "onVuzixMessage Peer: " + from + " Msg: " + msg);


        try {
            JSONObject data = new JSONObject(msg);
            Log.d("JSON", data.toString());
            String sHandler = data.getString("handler");
            if (sHandler.compareTo("message")==0){
                final String fn = data.getString("dispname");
                final String fmsg = data.getString("data");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,"" + fn + ": " + fmsg, Toast.LENGTH_LONG).show();
                });

            }else if(sHandler.compareTo("request")==0){
                String cmd = data.getString("type");
                if(cmd.compareTo("play")==0){
                    message = peerHandler.obtainMessage();
                    message.what = PUBLISH;
                    message.sendToTarget();
                }else if(cmd.compareTo("stop")==0){
                    message = peerHandler.obtainMessage();
                    message.what = UNPUBLISH;
                    message.sendToTarget();
                }else if(cmd.compareTo("imu_on")==0){
                    if(mRotationVectorSensor != null) {
                        Log.i(TAG, "registering IMU listener");
                        mSensorManager.registerListener(MainActivity.this,mRotationVectorSensor,200000);
                    }else{
                        Log.e(TAG, "mRotationVectorSensor is NULL");
                    }
                }else if(cmd.compareTo("imu_off")==0){
                    if(mRotationVectorSensor != null) {
                        // unregister sensor events, channel is gone
                        Log.i(TAG, "unregistering IMU listener");
                        mSensorManager.unregisterListener(MainActivity.this, mRotationVectorSensor);
                    }else{
                        Log.e(TAG, "mRotationVectorSensor is NULL");
                    }
                }else if(cmd.compareTo("invite")==0){
                    Log.d(TAG, "Invited by portal");
                    String requesterId = data.getString("dispname");
                    p2PClient.removeAllowedRemotePeer(peerId);
                    callRequest(requesterId);

                }else if(cmd.compareTo("uninvite")==0){
                    Log.d(TAG, "Uninvited by portal");

                    // unregister sensor
                    if(mRotationVectorSensor != null) {
                        mSensorManager.unregisterListener(MainActivity.this, mRotationVectorSensor);
                        mRotationVectorSensor = null;
                    }

                    if(mStepSensor != null){
                        mSensorManager.unregisterListener(MainActivity.this, mStepSensor);
                        mStepSensor = null;
                    }

                    if(mLightSensor != null){
                        mSensorManager.unregisterListener(MainActivity.this, mLightSensor);
                        mLightSensor = null;
                    }

                    // unregister battery events
                    if(mBroadcastReceiver != null) {
                        try {
                            MainActivity.this.unregisterReceiver(mBroadcastReceiver);
                        }catch (Exception ex){
                            Log.i(TAG, ex.getMessage());
                        }
                    }

                    if (statsTimer != null) {
                        statsTimer.cancel();
                    }

                    message = peerHandler.obtainMessage();
                    message.what = DISCONNECT;
                    message.sendToTarget();


                }
            }else if(sHandler.compareTo("draw")==0){
                fnDrawHandler(data);
            }

        }catch (Exception e){
            Log.e(TAG, "onDataReceived Error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // we received a sensor event. it is a good practice to check
        // that we received the proper event
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.
            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix , event.values);

            message = peerHandler.obtainMessage();
            message.what = SEND_IMU;
            message.sendToTarget();

            Log.i("ROT", Arrays.toString(mRotationMatrix));
        }else if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Log.d("STEPS", Arrays.toString(event.values));
            if(event.values[0]>0) {
                mSteps[0] = (int) event.values[0];
            }

            message = peerHandler.obtainMessage();
            message.what = SEND_STEPS;
            message.sendToTarget();

        }else if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            Log.d("LIGHT", Arrays.toString(event.values));
            mAmbientLight[0] = event.values[0];
            message = peerHandler.obtainMessage();
            message.what = SEND_AMB;
            message.sendToTarget();

        }else if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            Log.d("TEMPERATURE", Arrays.toString(event.values));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class PeerHandler extends Handler {
        public PeerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            JSONObject responsePacket = new JSONObject();
            switch (msg.what) {

                case LOGIN:
                    Log.d(TAG, "handleMessage: Login");

                    connectRequest(serverText.getText().toString(),
                            myIdText.getText().toString());

                    break;
                case LOGOUT:
                    Log.d(TAG, "handleMessage: Logout");

                    break;
                case INVITE:
                    Log.d(TAG, "handleMessage: INVITE");
                    runOnUiThread( ()-> {
                        Toast.makeText(MainActivity.this,
                                "onConnect Started ", Toast.LENGTH_SHORT).show();
                    });


                    try {
                        responsePacket.put("handler", "response");
                        responsePacket.put("action", "invite-accepted");
                        String sDataMessage = responsePacket.toString();
                        Log.d(TAG,"SendTO: " + peerId + " JSON: " + sDataMessage);

                        socketChannel.sendVuzixMessage(peerId, sDataMessage, new ActionCallback<Void>(){

                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Sent Message Successfully");

                                // initialize the rotation matrix to identity
                                mRotationMatrix[ 0] = 1;
                                mRotationMatrix[ 4] = 1;
                                mRotationMatrix[ 8] = 1;
                                mRotationMatrix[12] = 1;

                                // get sensor and register for events
                                mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

                                mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                                mSensorManager.registerListener(MainActivity.this,mStepSensor,1000000);

                                mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                                if(mLightSensor != null) {
                                    mSensorManager.registerListener(MainActivity.this, mLightSensor, 1000000);
                                }else{
                                    Log.e(TAG,"Light sensor is null");
                                }

                                // mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

                                // register for battery events
                                MainActivity.this.registerReceiver(mBroadcastReceiver,iFilter);
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, "Failed to Send Response: " + owtError.errorMessage);
                            }
                        });
                    }catch (JSONException je){
                        Log.e(TAG, je.getMessage());
                    }



                    break;
                case DISCONNECT:
                    Log.d(TAG, "handleMessage: DISCONNECT");
                    runOnUiThread( () -> {
                        Toast.makeText(MainActivity.this,
                                "onConnect Stopped", Toast.LENGTH_SHORT).show();
                    });


                    try {
                        responsePacket.put("handler", "response");
                        responsePacket.put("action", "disconnected");
                        String sDataMessage = responsePacket.toString();
                        Log.d(TAG,"SendTO: " + peerId + " JSON: " + sDataMessage);
                        // p2pClient.send
                        socketChannel.sendVuzixMessage(peerId, sDataMessage, new ActionCallback<Void>(){

                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Sent Disconnect Successfully");
                                onConnectSucceed();
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, "Failed to Send Response: " + owtError.errorMessage);
                            }
                        });
                    }catch (JSONException je){
                        Log.e(TAG, je.getMessage());
                    }
                    break;
                case INVITEME:
                    Log.d(TAG, "handleMessage: INVITEME");

                    break;
                case STOP:
                    Log.d(TAG, "handleMessage: STOP");

                    break;
                case PUBLISH:
                    Log.d(TAG, "handleMessage: PUBLISH");
                    p2PClient.publish(peerId, localStream, new ActionCallback<Publication>() {
                        @Override
                        public void onSuccess(Publication result) {
                            inCalling = true;
                            publication = result;

                            if (statsTimer != null) {
                                statsTimer.cancel();
                                statsTimer = null;
                            }
                            statsTimer = new Timer();
                            statsTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    getStats();
                                }
                            }, 0, STATS_INTERVAL_MS);
                        }

                        @Override
                        public void onFailure(OwtError error) {
                            Log.e(TAG,error.errorMessage);
                        }
                    });
                    break;
                case UNPUBLISH:
                    Log.d(TAG, "handleMessage: UNPUBLISH");
                    if (publication != null) {
                        publication.stop();
                        publication = null;
                        inCalling = false;
                    }

                    break;
                case SEND_DATA:
                    Log.d(TAG, "handleMessage: SEND_DATA");

                    break;
                case SEND_IMU:
                    Log.d(TAG, "handleMessage: SEND_IMU");
                    try {
                        JSONObject imuData = new JSONObject();
                        imuData.put("handler", "imu");
                        imuData.put("data",  Arrays.toString(mRotationMatrix));
                        String sDataImu = imuData.toString();
                        socketChannel.sendVuzixMessage(peerId, sDataImu, new ActionCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "IMU sent successfully");
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, owtError.errorMessage);
                            }

                        });
                    }catch (Exception ex){

                        Log.e("SEND_IMU", ex.getMessage());
                    }
                    break;
                case SEND_STEPS:
                    Log.d(TAG, "handleMessage: SEND_STEPS");
                    try {
                        JSONObject stepData = new JSONObject();
                        stepData.put("handler", "steps");
                        stepData.put("data",  Arrays.toString(mSteps));
                        String sDataSteps = stepData.toString();
                        Log.d("JSON", sDataSteps);
                        socketChannel.sendVuzixMessage(peerId, sDataSteps, new ActionCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "steps sent successfully");
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, owtError.errorMessage);
                            }


                        });
                    }catch (Exception ex){
                        Log.e("SEND_STEPS", ex.getMessage());
                    }
                    break;
                case SEND_AMB:
                    Log.d(TAG, "handleMessage: SEND_AMB");
                    try {
                        JSONObject lightData = new JSONObject();
                        lightData.put("handler", "light");
                        lightData.put("data",  Arrays.toString(mAmbientLight));
                        String sDataLight = lightData.toString();
                        Log.d("JSON", sDataLight);
                        socketChannel.sendVuzixMessage(peerId, sDataLight, new ActionCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "AMB sent successfully");
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, owtError.errorMessage);
                            }


                        });
                    }catch (Exception ex){

                        Log.e("SEND_AMB", ex.getMessage());
                    }

                    break;
                case SEND_BAT:
                    Log.d(TAG, "handleMessage: SEND_BAT");
                    try {
                        JSONObject batData = new JSONObject();
                        batData.put("handler", "battery");
                        batData.put("data",  Arrays.toString(mBattery));
                        String sDataBattery = batData.toString();
                        Log.d(TAG,"SendTO: " + peerId + " JSON: " + sDataBattery);
                        socketChannel.sendVuzixMessage(peerId, sDataBattery, new ActionCallback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "BAT sent successfully");
                            }

                            @Override
                            public void onFailure(OwtError owtError) {
                                Log.e(TAG, owtError.errorMessage);
                            }

                        });
                    }catch (Exception ex){

                        Log.e("SEND_BAT", ex.getMessage());
                    }
                    break;
                default:
                    Log.d(TAG, "handleMessage: UNKNOWN");
                    break;
            }
            super.handleMessage(msg);

        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            /*
                BatteryManager
                    The BatteryManager class contains strings and constants used for values in the
                    ACTION_BATTERY_CHANGED Intent, and provides a method for querying battery
                    and charging properties.
            */
            /*
                public static final String EXTRA_SCALE
                    Extra for ACTION_BATTERY_CHANGED: integer containing the maximum battery level.
                    Constant Value: "scale"
            */
            // Get the battery scale
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            // Display the battery scale in TextView
            Log.d("BATTERY","Battery Scale : " + scale);
            mBattery[0] = scale;
                        /*
                public static final String EXTRA_LEVEL
                    Extra for ACTION_BATTERY_CHANGED: integer field containing the current battery
                    level, from 0 to EXTRA_SCALE.

                    Constant Value: "level"
            */
            // get the battery level
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            // Display the battery level in TextView
            Log.d("BATTERY","Battery Level : " + level);
            mBattery[1] = level;

            // Calculate the battery charged percentage
            float percentage = level/ (float) scale;
            // Update the progress bar to display current battery charged percentage
            int chargePerc = (int)((percentage)*100);
            mBattery[2] = chargePerc;

            // Show the battery charged percentage text inside progress bar
            Log.d("BATTERY","Battery Charge : " + chargePerc + "%");

            message = peerHandler.obtainMessage();
            message.what = SEND_BAT;
            message.sendToTarget();

        }
    };

}
