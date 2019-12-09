package com.example.watchtest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.tasks.Tasks;

import java.net.Authenticator;
import java.util.concurrent.ExecutionException;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends WearableActivity implements
        DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String LOG_TAG = "Wearable";
    private static final String COUNT_KEY = "com.example.count";
    private DataClient mDataClient;

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private Integer currentValue = 0;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on
        setAmbientEnabled();

        //mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);

//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
//                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) this)
//                .addApi(Wearable.API)
//                .build();
//
//        mGoogleApiClient.connect();

        // SensorManager
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor((Sensor.TYPE_HEART_RATE));
        // Initialize API client for sending data to phone
        mDataClient = Wearable.getDataClient(this);
    }

    @Override
    protected void  onStart() {
        super.onStart();

        if (mHeartRateSensor != null) {
            Log.d(LOG_TAG, "HEART RATE SENSOR NAME: " + mHeartRateSensor.getName() + " TYPE: "
            + mHeartRateSensor.getType());
            mSensorManager.unregisterListener((SensorEventListener) this, this.mHeartRateSensor);
            boolean isRegistered = mSensorManager.registerListener((SensorEventListener) this,
                    mSensorManager.getDefaultSensor(21), 2);

            Log.d(LOG_TAG, "HEART RATE LISTENER REGISTERED: " + isRegistered);
        } else{
            Log.d(LOG_TAG, "NO HEART RATE SENSOR");
        }
        mDataClient.Wearable.getDataClient(this);
    }


    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values.length > 0) {
            for (Float value : sensorEvent.values) {
                int newValue = Math.round(value);
                if(currentValue != newValue ) {
                    currentValue = newValue;
                    mTextView.setText(currentValue.toString());
                  //  sendMessageToHandheld(currentValue.toString());
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSensorManager.unregisterListener(this);
        Log.d(LOG_TAG, "SENSOR UNREGISTERED");
    }

    private void sendMessageToHandheld(final String message) {
        if (mGoogleApiClient == null)
            return;
        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                final String path = "heartRate";

                for (Node node : nodes) {
                    Log.d(LOG_TAG, "SEND MESSAGE TO HANDHELD: " + message);

                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, data);
                }
            }
        });
    }
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        //updateDisplay();
    }
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        //updateDisplay();
    }
    @Override
    public void onExitAmbient() {
        //updateDisplay();
        super.onExitAmbient();
    }
//    private void updateDisplay() {
//        if (isAmbient()) {
//            mContainerView.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
//            mTextView.setTextColor(ContextCompat.getColor(this, R.color.white));
//        } else {
//            mContainerView.setBackground(null);
//            mTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
//        }
//    }

    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(LOG_TAG, "ACCURACY CHANGED: " + i);
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event: dataEventBuffer){
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem Delete: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem Changed : " + event.getDataItem().getUri());
            }
        }

    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {

    }
}//End MainActivity
