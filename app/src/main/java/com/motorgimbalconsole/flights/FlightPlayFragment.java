package com.motorgimbalconsole.flights;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
    private ConsoleApplication myBT;
    private PApplet rocket;
    private FlightData myflight = null;
    XYSeriesCollection allFlightData;
    String FlightName;
    Thread altiFlightPlay;
    boolean status = true;
    boolean pause = false;
    boolean ViewCreated = false;
    Button btnPlay;
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
        view = inflater.inflate(R.layout.fragment_play_flight, container, false);
        pause = false;

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
                Activity activity = getActivity();
                //Toast.makeText(activity.getBaseContext(), "play clicked", Toast.LENGTH_LONG).show();
                if(pause)
                    pause =false;
                else
                    pause = true;
            }
        });
        ViewCreated = true;
        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        fragment.requestDraw();
        view.refreshDrawableState();
        view.bringToFront();
    }
    @Override
    public void onResume() {
        super.onResume();
        fragment.onResume(); //perahps we can remove that
        //This is the only way I can redraw the rocket after leaving the tab
        getChildFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
    }
}
