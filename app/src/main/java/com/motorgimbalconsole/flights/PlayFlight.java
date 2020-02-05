package com.motorgimbalconsole.flights;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.Rocket;
import com.motorgimbalconsole.flights.FlightData;
import com.motorgimbalconsole.flights.FlightViewActivity;

import org.afree.data.xy.XYSeriesCollection;

import java.util.concurrent.TimeUnit;

import processing.android.PFragment;
import processing.core.PApplet;

/**
 * @description: This will allow play back of the current flight
 * @author: boris.dureau@neuf.fr
 **/
public class PlayFlight extends AppCompatActivity {
    private PApplet rocket;
    Button btnDismiss;
    String FlightName = null;
    ConsoleApplication myBT;
    private FlightData myflight = null;
    XYSeriesCollection allFlightData;
    Thread altiFlightPlay;
    boolean ViewCreated = false;
    boolean status = true;
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_flight);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightViewActivity.SELECTED_FLIGHT);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = myflight.GetFlightData(FlightName);
        btnDismiss = (Button)findViewById(R.id.btnDismiss);
        rocket = new Rocket();

        PFragment fragment = new PFragment(rocket);
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status =false;
                finish();      //exit the activity
            }
        });


        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true){
                    if(!status) break;
                    //myBT.ReadResult(10000);
                    float X=0, Y=0,Z=0;
                    long time, cumTime=0, prevTime=0 ;

                    for(int i=0; i < allFlightData.getSeries("Euler X").getItemCount(); i++) {
                    //for(int i=0; i < 10; i++) {
                        time = allFlightData.getSeries("Euler X").getX(i).longValue();
                        cumTime = time - prevTime;
                        prevTime = time;
                        try {
                            TimeUnit.MILLISECONDS.sleep(cumTime);
                        }
                        catch (InterruptedException ex){

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
        ViewCreated = true;
    }

}
