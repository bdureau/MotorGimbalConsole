//package com.altimeter.bdureau.bearconsole;
package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private ProgressDialog progress;

    private Button buttonDismiss;

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the flight name
            String currentFlight = ((TextView) v).getText().toString();

            // Make an intent to start next activity.
            Intent i = new Intent(FlightListActivity.this, FlightViewActivity.class);

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
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

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

    private void getFlights() {

        //get flights
        if (myBT.getConnected()) {
                //clear anything on the connection
                myBT.flush();
                myBT.clearInput();
                myBT.getFlightData().ClearFlight();
                // Send command to retrieve the config
                myBT.write("a;\n".toString());
                myBT.flush();

        try {
            //wait for data to arrive
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {
           // msg("Failed to retrieve flights");
        }
       // if (myBT.getConnected()) {
            String myMessage = "";
            myBT.setDataReady(false);
           // myBT.initFlightData();
            myMessage = myBT.ReadResult(60000);

            if (myMessage.equals("start end")) {

                flightNames = new ArrayList<String>();

                myflight = myBT.getFlightData();
                flightNames = myflight.getAllFlightNames2();
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void appendLog(String text) {
        //File logFile = new File("sdcard/debugfile.txt");
        //File logFile = new File(Environment.getDataDirectory() +"/debugfile.txt");
        //getExternalStorageDirectory()

        //File logFile = new File(Environment.getExternalStorageDirectory().toString() ,"debugfile.txt");
        File logFile = new File(Environment.getExternalStorageState().toString() ,"debugfile.txt");

        //Environment.getDownloadCacheDirectory()
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void appendLog2 () {
       /*FileOutputStream stream=null;

        File directory = new File(Environment.getExternalStorageState()+"/toto");
        try {
            if(!directory.exists()) {
                directory.createNewFile();
            }
            File dataFile = new File(directory, "toto.txt");
            stream = new FileOutputStream(dataFile, true); // true if append is required.
            stream.write("test".getBytes());
            stream.flush();
            if (null != stream) {
                stream.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            //if (null != stream) {
            // stream.close();
            //}
        }*/

        FileOutputStream fos ;

        try {
            fos = new FileOutputStream("/sdcard/filename.txt", true);

            FileWriter fWriter;

            try {
                fWriter = new FileWriter(fos.getFD());
                fWriter.write("hi");
                fWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                fos.getFD().sync();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private class RetrieveFlights extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private  AlertDialog.Builder builder=null;
        private AlertDialog alert;
        @Override
        protected void onPreExecute()
        {
            //"Retrieving flights..."
            //"Please wait!!!"
           /* progress = ProgressDialog.show(FlightListActivity.this,
                    getResources().getString(R.string.msg7),
                    getResources().getString(R.string.msg8));  //show a progress dialog
*/

            builder = new AlertDialog.Builder(FlightListActivity.this);
            //Retrieving flights...
            builder.setMessage(getResources().getString(R.string.msg7))
                    .setTitle(getResources().getString(R.string.msg8))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.Flight_list_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                            myBT.setExit(true);
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            getFlights();
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
            //if(Environment.getExternalStorageState()!=null)
               // msg(Environment.getExternalStorageDirectory().toString());
               // appendLog2();
            //progress.dismiss();
            alert.dismiss();
            if (myflight.getNbrOfFlight()==0 )
                msg(getResources().getString(R.string.FL_msg9));
        }
    }

}
