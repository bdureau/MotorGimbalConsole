package com.motorgimbalconsole.flights;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    // Point 1 : Gestion de l'exécution en arrière-plan sans AsyncTask
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean canceled = false;

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

        // Point 1 : Lancement de la récupération sans AsyncTask
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
        backgroundExecutor.shutdownNow(); // Point 1 : Nettoyage du thread d'arrière-plan
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    // Point 1 : Remplacement de RetrieveFlights extends AsyncTask
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

            // Point 2 : Suppression de l'attente active bloquante (while (available <= 0);)

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

                    // Point 2 : Suppression de l'attente active bloquante (while (available <= 0);)

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

            // Point 3 : Sécurisation lors de l'annulation (vérification que la liste n'est pas vide)
            if (canceled) {
                //order the names in the collection
                Collections.sort(flightNames);
                //remove the last flight which might have incomplete data
                if (!flightNames.isEmpty()) {
                    flightNames.remove(flightNames.size() - 1);
                }
            }

            // Point 3 : Protection contre la division par zéro (dt <= 0) pour la vitesse
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

            // Point 3 : Protection contre la division par zéro (dt <= 0) pour l'accélération
            for (String flight : flightNames) {
                XYSeries serie = myflight.GetFlightData(flight).getSeries(17);
                int nbrData = serie.getItemCount();
                for (int i = 1; i < nbrData; i++) {
                    double dt = (serie.getX(i).doubleValue() - serie.getX(i - 1).doubleValue()) / 1000.0;
                    if (dt > 0.0) {
                        double X = serie.getX(i).doubleValue();
                        double Y = abs(serie.getY(i).doubleValue() - serie.getY(i - 1).doubleValue()) / dt;
                        myflight.AddToFlight((long) X, (long) (Y), flight, 18);
                    }
                }
            }
        }

        // Point 1 : Notification sur le thread UI pour finaliser l'affichage
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