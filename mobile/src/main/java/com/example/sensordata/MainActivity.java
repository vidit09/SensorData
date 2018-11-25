package com.example.sensordata;

import android.app.Activity;
import android.media.AudioAttributes;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;

import android.media.SoundPool;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/** Mobile Activity
 This listens for the step count provided by Wear device.
 Keeps the track of the timestamp of the steps and calculates the
 approx steps per second by the runner.

 For the demo, we have default audio with 142 beats per minute. The application
 modulates the sampling rate of the audio of match the steps per second taken by
 runner.
 **/
public class MainActivity extends Activity implements
        DataClient.OnDataChangedListener {

    private String COUNT_KEY = "STEP_COUNT";
    // Maintain history of steps and timestamp
    private Queue<Integer> countHist = new LinkedList<Integer>();
    private Queue<Long> timeHist = new LinkedList<Long>();
    //Handlinf audio playback for different sampling rate
    private SoundPool sounds;
    private int streamId;
    private boolean loaded = false;
    //Reference for new steps per second calculation
    private Integer refCount;
    private Long refStep;
    //Parameters choosen for the song.
    private Integer window = 10;
    private int BPS  = 2;//142; //known
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String separator = "/";

        //Get the audio ready and play in loop.
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
        sounds.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                streamId = soundPool.play(sampleId, 1.0f, 1.0f, 0, -1, 1.0f);
                loaded = true;
            }
        });
        sounds.load(this, R.raw.test, 1);
    }


    @Override
    protected  void onResume(){
        super.onResume();
        Wearable.getDataClient(this).addListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }
    //change the audio playback according to sps
    private void changeFreq(double sps){
        float scale = (float)sps/BPS;
        float factor = scale;
//        float scale = (float)sps/BPS;
//        factor = scale>0?scale: ;
//        if (BPS < (int)sps+2){
//            factor = 1.5f;
//        }else if(BPS > (int)sps-2){
//            factor = 0.5f;
//        }
//        else
//            factor = 1.0f;

        if(loaded){
            sounds.setRate(streamId,factor);
        }
    }

    //Using regression find the current window sps
    private double findSPS(){
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        double[] xa = new double[window];
        double[] ya = new double[window];
        int count = 0;
        for(Integer y:countHist){
            ya[count] = (double)(y-refCount);
            sumy += y-refCount;
        }

        count = 0;
        for(Long x:timeHist){
            double v = (x-refStep)/1000000000.0;
            xa[count] = v;
            sumx += v ;
//            Log.d("Main",String.valueOf(v));
//            sumx2 += v*v;
        }

        double xbar = sumx / window;
        double ybar = sumy / window;

        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < window; i++) {
            xxbar += (xa[i] - xbar) * (xa[i] - xbar);
            yybar += (ya[i] - ybar) * (ya[i] - ybar);
            xybar += (xa[i] - xbar) * (ya[i] - ybar);
        }
        double beta1 = xybar / xxbar;
        double beta0 = ybar - beta1 * xbar;

        return beta1;

    }
    //Keep the history to size window
    private void updateCount(String str){
        Log.d("Main Activity","Message from the watch "+str);
        String[] parts = str.split("_");
        Integer count = Integer.valueOf(parts[1]);
        Long step = Long.parseLong(parts[2]);

        if (!countHist.isEmpty() && countHist.size() == window) {
            if (!countHist.isEmpty())
                countHist.poll();

            if (!timeHist.isEmpty())
                timeHist.poll();
        }

        countHist.add(count);
        timeHist.add(step);

        refCount = countHist.peek();
        refStep = timeHist.peek();

        Log.d("Main","size window"+String.valueOf(countHist.size()));
        if(countHist.size()==window) {
            double observed_sps = findSPS();
            Log.d("Main Activity", "BPS: " + String.valueOf(observed_sps));


            changeFreq(observed_sps);
        }

//        String cur_count_string = str.replace("Count: "," ");
//        Integer curr_count = Integer.valueOf(cur_count_string);
//        countHist.add(curr_count);
//        Log.d("Main Activity","Hist Size: "+String.valueOf(countHist.size()));
    }

    //Listener for the Wear Device
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/count") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateCount(dataMap.getString(COUNT_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }



}
