package com.motorgimbalconsole.flights;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.utils.Rocket;

import org.afree.data.xy.XYSeriesCollection;

import java.util.concurrent.TimeUnit;

import processing.android.PFragment;
import processing.core.PApplet;

public class FlightPlayFragment extends Fragment {
    private String TAG = "FlightPlayFragment";
    private ConsoleApplication myBT;
    private PApplet rocket;
    private FlightData myflight = null;
    private XYSeriesCollection allFlightData;
    private String FlightName;
    private Thread altiFlightPlay;
    private boolean status = true;
    private boolean pause = false;
    private boolean ViewCreated = false;
    private Button btnPlay;
    private View view;
    private PFragment fragment;

    public FlightPlayFragment(ConsoleApplication pBT,
                              XYSeriesCollection pAllFlightData,
                              String pFlightName) {
        myBT = pBT;
        allFlightData = pAllFlightData;
        FlightName = pFlightName;
    };

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        view = inflater.inflate(R.layout.fragment_play_flight, container, false);
        pause = false;
        status = true;

        btnPlay = (Button) view.findViewById(R.id.butPlay);

        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = myflight.GetFlightData(FlightName);
        rocket = new Rocket();

        fragment = new PFragment(rocket);
        getChildFragmentManager().beginTransaction().add(R.id.container, fragment).commit();

        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true){
                    if(!status) break;

                    float X=0, Y=0,Z=0;
                    long time, cumTime=0, prevTime=0 ;

                    for(int i=0; i < allFlightData.getSeries("Euler X").getItemCount(); i++) {

                        time = allFlightData.getSeries("Euler X").getX(i).longValue();
                        cumTime = time - prevTime;
                        prevTime = time;
                        try {
                            TimeUnit.MILLISECONDS.sleep(cumTime);
                        }
                        catch (InterruptedException ex){

                        }
                        while(pause){

                        }
                        X =allFlightData.getSeries("Euler X").getY(i).floatValue();
                        Y =allFlightData.getSeries("Euler Y").getY(i).floatValue();
                        Z =allFlightData.getSeries("Euler Z").getY(i).floatValue();
                        if (ViewCreated)
                            ((Rocket) rocket).setInputString( X,  Y,  Z, time);
                    }
                }
            }
        };

        altiFlightPlay = new Thread(r);
        altiFlightPlay.start();



        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Activity activity = getActivity();
                //Toast.makeText(activity.getBaseContext(), "play clicked", Toast.LENGTH_LONG).show();

                if(pause) {
                    pause =false;
                    Log.d(TAG, "pause is false");
                    //altiFlightPlay.suspend();
                }
                else {
                    pause = true;
                    Log.d(TAG, "pause is true");
                    //altiFlightPlay.resume();
                }

            }
        });
        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        rocket.onDestroy();
    }
    @Override
    public void onStop() {
        super.onStop();
        status = false;
        pause = false;
        //altiFlightPlay.interrupt();
        //altiFlightPlay.destroy();
        Log.d(TAG, "onStop");
        fragment.onStop();
        //rocket.onStop();
        rocket.onDestroy();
    }
    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        fragment.requestDraw();
        view.refreshDrawableState();
        view.bringToFront();
    }
    @Override
    public void onPause() {
        status = false;
        pause = false;
        Log.d(TAG, "onPause");
        super.onPause();
        fragment.onPause();
    }
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        fragment.onResume(); //perhaps we can remove that
        rocket.onResume();//new
        //This is the only way I can redraw the rocket after leaving the tab
        getChildFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
    }
}
