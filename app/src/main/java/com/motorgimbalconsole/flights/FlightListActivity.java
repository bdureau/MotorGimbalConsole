package com.motorgimbalconsole.flights;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import org.afree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;

/**
 *   @description: This retrieve the flight list
 *   @author: boris.dureau@neuf.fr
 **/
public class FlightListActivity extends AppCompatActivity {
    public static String SELECTED_FLIGHT = "MyFlight";

    ListView flightList = null;
    ConsoleApplication myBT;
    List<String> flightNames = null;
    private FlightData myflight = null;
    private AlertDialog alert;

    private Button buttonDismiss;

    // Background execution of the flight download (replaces the former AsyncTask)
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean canceled = false;

    @Override
    protected void attachBaseContext(Context base) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = base.getSharedPreferences("GimbalConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(base, lang));
    }
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the flight name
            String currentFlight = ((TextView) v).getText().toString();
            // Make an intent to start next activity.
            Intent i = new Intent(FlightListActivity.this, FlightViewTabActivity.class);
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

        setContentView(R.layout.activity_flight_list);
        buttonDismiss = (Button) findViewById(R.id.butDismiss);

        // Start the download in the background (replaces the former AsyncTask)
        startRetrieveFlights();

        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canceled = true;
                finish();      //exit the activity
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        canceled = true;
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        backgroundExecutor.shutdownNow(); // Stop the background download thread
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    // Flight download, run in the background (replaces the former RetrieveFlights AsyncTask)
    private void startRetrieveFlights() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FlightListActivity.this);
        //Retrieving flights...
        builder.setMessage(getResources().getString(R.string.msg7))
                .setTitle(getResources().getString(R.string.msg8))
                .setCancelable(false)
                .setNegativeButton(getResources().getString(R.string.Flight_list_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        canceled = true;
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();

        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doRetrieveFlightsInBackground();
            }
        });
    }

    private void doRetrieveFlightsInBackground() {
        int nbrOfFlight = 0;
        //get flights
        if (myBT.getConnected()) {
            //clear anything on the connection
            myBT.flush();
            myBT.clearInput();
            // clear flight object
            myBT.getFlightData().ClearFlight();
            //retrieve the number of flight
            myBT.write("n;");
            myBT.flush();

            // No busy-wait (while (available() <= 0);): ReadResult() waits itself, with a timeout

            String myMessage1 = "";
            myBT.setDataReady(false);
            myBT.initFlightData();

            myMessage1 = myBT.ReadResult(60000);

            if (myMessage1.equals("start nbrOfFlight end")) {
                nbrOfFlight = myBT.getNbrOfFlights();
            }

            if (nbrOfFlight > 0) {
                // Send command to retrieve the config
                for (int i = 0; i < nbrOfFlight; i++) {

                    dialogAppend(getString(R.string.retrieving_flight) + (i + 1));
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("r" + i + ";");
                    myBT.flush();

                    // No busy-wait here: ReadResult() waits for the answer itself, with a timeout

                    String myMessage = "";
                    myBT.setDataReady(false);

                    myMessage = myBT.ReadResult(60000);

                    if (myMessage.equals("start end")) {

                    }
                    //if canceled then exit the for loop
                    if (canceled)
                        i = nbrOfFlight;
                }
            }

            flightNames = new ArrayList<String>();

            myflight = myBT.getFlightData();
            flightNames = myflight.getAllFlightNames2();

            // Cancelled download: the last flight may be incomplete and is dropped (only if the
            // list is not empty)
            if (canceled) {
                //order the names in the collection
                Collections.sort(flightNames);
                //remove the last flight which might have incomplete data
                if (!flightNames.isEmpty()) {
                    flightNames.remove(flightNames.size() - 1);
                }
            }

            // Speed curve (FlightData.SERIE_SPEED): absolute vertical speed between two altitude
            // samples; samples with the same time stamp are skipped (no division by zero)
            for (String flight : flightNames) {
                XYSeries serie = myflight.GetFlightData(flight).getSeries(getResources().getString(R.string.altitude));
                int nbrData = serie.getItemCount();
                for (int i = 1; i < nbrData; i++) {
                    double dt = (serie.getX(i).doubleValue() - serie.getX(i - 1).doubleValue()) / 1000.0;
                    if (dt > 0.0) {
                        double X = serie.getX(i).doubleValue();
                        double Y = abs(serie.getY(i).doubleValue() - serie.getY(i - 1).doubleValue()) / dt;
                        myflight.AddToFlight((long) X, (long) (Y), flight, 17);
                    }
                }
            }

            // Acceleration curve (FlightData.SERIE_ACCEL): it is filled while the flight is
            // downloaded, from the IMU accelerometer, in G (see ConsoleApplication, "$data"
            // sentence, and AccelerationUtils). The former second derivative of the altitude
            // computed here was removed: it was mixed into the same curve, in m/s^2, and a
            // sample-by-sample double derivative of a barometric altitude is mostly noise.
        }

        // Back on the UI thread to display the list of flights
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onRetrieveFlightsFinished();
            }
        });
    }

    private void onRetrieveFlightsFinished() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        final ArrayAdapter adapter = new ArrayAdapter(FlightListActivity.this, android.R.layout.simple_list_item_1, flightNames);
        adapter.sort(new Comparator<String>() {
            public int compare(String object1, String object2) {
                return object1.compareTo(object2);
            }
        });

        flightList = (ListView) findViewById(R.id.listViewFlightList);
        flightList.setAdapter(adapter);
        flightList.setOnItemClickListener(myListClickListener);

        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        if (canceled)
            msg(getString(R.string.flight_retrieve_canceled));
        if (myflight != null && myflight.getNbrOfFlight() == 0 && !canceled)
            msg(getResources().getString(R.string.FL_msg9));
    }

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (alert != null && alert.isShowing()) {
                    alert.setMessage(ftext);
                }
            }
        });
    }
}