package com.example.watchtest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.wear.widget.BoxInsetLayout;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.tasks.Tasks;

import java.net.Authenticator;
import java.util.concurrent.ExecutionException;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends WearableActivity implements
        SensorEventListener
{

    private static final String LOG_TAG = "Wearable";

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

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor((Sensor.TYPE_HEART_RATE));
    }

    @Override
    protected void  onStart() {
        super.onStart();

        if (mHeartRateSensor != null) {
            Log.d(LOG_TAG, "HEART RATE SENSOR NAME: " + mHeartRateSensor.getName() + " TYPE: "
            + mHeartRateSensor.getType());
            mSensorManager.unregisterListener(this, this.mHeartRateSensor);
            boolean isRegistered = mSensorManager.registerListener(this, this.mHeartRateSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            Log.d(LOG_TAG, "HEART RATE LISTENER REGISTERED: " + isRegistered);
        } else{
            Log.d(LOG_TAG, "NO HEART RATE SENSOR");
        }
        sendMessageToHandheld("0");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE && sensorEvent.values.length > 0) {
            for (Float value : sensorEvent.values) {
                int newValue = Math.round(value);
                if(currentValue != newValue ) {
                    currentValue = newValue;
                    mTextView.setText(currentValue.toString());
                    sendMessageToHandheld(currentValue.toString());
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
        updateDisplay();
    }
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }
    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }
    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            mTextView.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(LOG_TAG, "ACCURACY CHANGED: " + i);
    }
}//End MainActivity
