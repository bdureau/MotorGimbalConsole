//package com.altimeter.bdureau.bearconsole;
package com.motorgimbalconsole.flights;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import org.afree.data.xy.XYSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

/**
 *   @description: This retrieve the flight list
 *   @author: boris.dureau@neuf.fr
 **/
public class FlightListActivity extends AppCompatActivity {
    public static String SELECTED_FLIGHT = "MyFlight";

    ListView flightList=null;
    ConsoleApplication myBT;
    List<String> flightNames = null;
    private FlightData myflight = null;
    //private ProgressDialog progress;
    private AlertDialog alert;


    private Button buttonDismiss;

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the flight name
            String currentFlight = ((TextView) v).getText().toString();

            // Make an intent to start next activity.
            Intent i ;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                //if android ver = 8 or greater use the MPlib
                //i = new Intent(FlightListActivity.this, FlightViewMPActivity.class);
                i = new Intent(FlightListActivity.this, FlightViewTabActivity.class);
            } else {
                if (myBT.getAppConf().getGraphicsLibType().equals("0"))
                    i = new Intent(FlightListActivity.this, FlightViewActivity.class);
                else
                    //i = new Intent(FlightListActivity.this, FlightViewMPActivity.class);
                    i = new Intent(FlightListActivity.this, FlightViewTabActivity.class);
            }
            //Change the activity.
            i.putExtra(SELECTED_FLIGHT, currentFlight);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_list);
        buttonDismiss =  (Button) findViewById(R.id.butDismiss);
        new RetrieveFlights().execute();
        buttonDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the activity
            }
        });
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }



    private class RetrieveFlights extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private AlertDialog.Builder builder=null;

        private Boolean canceled = false;
        @Override
        protected void onPreExecute()
        {


            builder = new AlertDialog.Builder(FlightListActivity.this);
            //Retrieving flights...
            builder.setMessage(getResources().getString(R.string.msg7))
                    .setTitle(getResources().getString(R.string.msg8))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.Flight_list_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            //myBT.setExit(true);
                            canceled = true;
                            dialog.cancel();

                        }
                    });
            alert = builder.create();

            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            int nbrOfFlight = 0;
            //get flights
            if (myBT.getConnected()) {
                //clear anything on the connection
                myBT.flush();
                myBT.clearInput();
                // clear flight object
                myBT.getFlightData().ClearFlight();
                //retrieve the number of flight
                myBT.write("n;".toString());
                myBT.flush();
                try {
                    //wait for data to arrive
                    while (myBT.getInputStream().available() <= 0) ;
                } catch (IOException e) {
                    // msg("Failed to retrieve flights");
                }

                String myMessage1 = "";
                myBT.setDataReady(false);

                myMessage1 = myBT.ReadResult(60000);

                if (myMessage1.equals("start nbrOfFlight end")) {
                    nbrOfFlight = myBT.getNbrOfFlights();
                }

                if (nbrOfFlight >0) {
                    // Send command to retrieve the config
                    for (int i =0; i < nbrOfFlight; i++) {

                        dialogAppend(getString(R.string.retrieving_flight) +(i+1));
                        myBT.write(("r"+ i+";").toString());
                        myBT.flush();

                        try {
                            //wait for data to arrive
                            while (myBT.getInputStream().available() <= 0) ;
                        } catch (IOException e) {
                            // msg("Failed to retrieve flights");
                        }

                        String myMessage = "";
                        myBT.setDataReady(false);

                        myMessage = myBT.ReadResult(60000);

                        if (myMessage.equals("start end")) {

                        }
                        //if canceled then exit the for loop
                        if(canceled)
                            i= nbrOfFlight;
                    }

                }
                flightNames = new ArrayList<String>();

                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();
                if (canceled) {
                    //order the names in the collection
                    Collections.sort(flightNames);
                    //remove the last flight which might have incomplete data
                    flightNames.remove(flightNames.size()-1);
                }
                //calculate the speed
                for (String flight : flightNames) {
                    XYSeries serie = myflight.GetFlightData(flight).getSeries(getResources().getString(R.string.altitude));
                    int nbrData = serie.getItemCount();
                    for (int i = 1; i < nbrData; i++) {
                        double X, Y;
                        X = serie.getX(i).doubleValue();
                        Y = abs(serie.getY(i).doubleValue() - serie.getY(i - 1).doubleValue()) / (((serie.getX(i).doubleValue() - serie.getX(i - 1).doubleValue()) / 1000));
                        myflight.AddToFlight((long) X, (long) (Y), flight, 17);
                    }
                }
                //calculate the acceleration
                for (String flight : flightNames) {
                    XYSeries serie = myflight.GetFlightData(flight).getSeries(17);
                    int nbrData = serie.getItemCount();
                    for (int i = 1; i < nbrData; i++) {
                        double X, Y;
                        X = serie.getX(i).doubleValue();
                        Y = abs(serie.getY(i).doubleValue() - serie.getY(i - 1).doubleValue()) / ((serie.getX(i).doubleValue() - serie.getX(i - 1).doubleValue()) / 1000);
                        myflight.AddToFlight((long) X, (long) (Y ), flight, 18);
                    }
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
                final ArrayAdapter adapter = new ArrayAdapter(FlightListActivity.this, android.R.layout.simple_list_item_1, flightNames);
                adapter.sort(new Comparator<String>() {
                    public int compare(String object1, String object2) {
                        return object1.compareTo(object2);
                    }
                });

                flightList = (ListView) findViewById(R.id.listViewFlightList);
                flightList.setAdapter(adapter);
                flightList.setOnItemClickListener(myListClickListener);
            alert.dismiss();
            if (canceled)
                msg(getString(R.string.flight_retrieve_canceled));
            if (myflight.getNbrOfFlight()==0 && !canceled)
                msg(getResources().getString(R.string.FL_msg9));
        }
    }
    Handler mHandler = new Handler();

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }
}
