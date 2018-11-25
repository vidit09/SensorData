package com.example.sensordata;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.support.wear.ambient.AmbientModeSupport;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Activity handles the communication of the sensor data to handheld device.
 * Instead of directly using the step counter, used the accelerometer data
 * to calculate the step count. The accelero meter to step count implementation taken from
 * http://www.gadgetsaint.com/android/create-pedometer-step-counter-android/#.W_oPzJNKg1I
 */
public class MainActivity extends FragmentActivity implements AmbientModeSupport.AmbientCallbackProvider,
        SensorEventListener,StepListener {
    //Simple text view shows the count and the timesteps
    private TextView mTextView;
    private SensorManager sensorManager;
    private Sensor sensor;
    private static final String TAG = "MainActivity";
    private DataClient mDataClient;
    private String COUNT_KEY = "STEP_COUNT";
    private String msg;

    //Important to keep device in foreground with ambient mode
    private AmbientModeSupport.AmbientController mAmbientController;
    private StepDetector simpleStepDetector;
    private int numSteps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAmbientController = AmbientModeSupport.attach(this);
        numSteps = 0;
        mTextView = (TextView) findViewById(R.id.text);

        sensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
//        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        mDataClient = Wearable.getDataClient(getApplicationContext());
        // Enables Always-on
//        setAmbientEnabled();
    }


    protected void onResume() {
        super.onResume();
//        sensorManager.registerListener(this,this.sensor,Sensor.REPORTING_MODE_ON_CHANGE);
        sensorManager.registerListener(this, this.sensor, 3);
    }
    //Send data to the handheld device
    private void increaseCounter() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        putDataMapReq.getDataMap().putString(COUNT_KEY, msg);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        Task<DataItem> putDataTask = mDataClient.putDataItem(putDataReq);
        putDataTask.addOnSuccessListener(
                new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.d(TAG, "Sending data was successful: " + dataItem);
                    }
                });
    }

    //register the change in accelerometer readings
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            msg = "Count: " + (int) event.values[0];
//            increaseCounter();

            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }

        }

    //Listener output for step count
    @Override
    public void step(long timeNs) {
        numSteps++;
        String step = String.valueOf(timeNs);
        msg = "Count:_"+ String.valueOf(numSteps)+"_"+step;
        increaseCounter();
        mTextView.setText(msg);
        Log.d(TAG, msg);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG,String.valueOf(i));
    }

        @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }

}
