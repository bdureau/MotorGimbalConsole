package com.motorgimbalconsole;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import processing.android.PFragment;
import processing.core.PApplet;

public class MainActivityScreen extends AppCompatActivity {
    private PApplet sketch;
    Button btnConnectDisconnect, btnOrientation, btnCalibrate;
    //PFragment fragment;
    private BluetoothConnection BTCon = null;
    private String address;
    ConsoleApplication myBT;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_screen);
        btnConnectDisconnect = (Button)findViewById(R.id.button);
        btnConnectDisconnect.setText("connect");

        btnCalibrate = (Button)findViewById(R.id.butCalibrate);
        btnOrientation = (Button)findViewById(R.id.butOrientation);
        DisableUI();
        //get the bluetooth and USB Application pointer
        myBT = (ConsoleApplication) getApplication();

        sketch = new Sketch();
        ((Sketch) sketch).setMyApp(myBT);
        //((Sketch) sketch).setApp(myBT);
        PFragment fragment = new PFragment(sketch);
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();



        //BTCon = new BluetoothConnection();
        //fragment =(PFragment)findViewById(R.id.fragment);
        /*FrameLayout frame = new FrameLayout(this);
        frame.setId(CompatUtils.getUniqueViewId());
        setContentView(frame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        sketch = new Sketch();
        PFragment fragment = new PFragment(sketch);
        fragment.setView(frame, this);*/

        btnOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Sketch) sketch).setOrientation('h');
            }
        });
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBT.write("c;\n".toString());
            }
        });

         btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (myBT.getConnected()) {
                    // msg("disconnecting");
                    Disconnect(); //close connection
                    DisableUI();
                    btnConnectDisconnect.setText("connect");
                }
                else {

                        address = myBT.getAddress();

                        if (address != null ) {
                            new ConnectBT().execute(); //Call the class to connect
                            if (myBT.getConnected()) {
                                EnableUI();
                                btnConnectDisconnect.setText("disconnect");
                            }
                        } else {
                            // choose the bluetooth device
                            Intent i = new Intent(MainActivityScreen.this, SearchBluetooth.class);
                            startActivity(i);
                        }


                }
            }
        });
    }

    private void DisableUI () {
        btnCalibrate.setEnabled(false);
        btnOrientation.setEnabled(false);

    }
    private void EnableUI () {
        btnCalibrate.setEnabled(true);
        btnOrientation.setEnabled(true);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (sketch != null) {
            sketch.onRequestPermissionsResult(
                    requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (sketch != null) {
            sketch.onNewIntent(intent);
        }
    }
    private void Disconnect() {
        myBT.Disconnect();
    }
    // connect to the bluetooth adapter
    public boolean connect() {
        boolean state=false;

        state = BTCon.connect(address);

           /* if(!isConnectionValid()){
                Disconnect();
                state = false;
            }*/

        return state;
    }
    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    /* This is the Bluetooth connection sub class */
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            //"Connecting...", "Please wait!!!"
            progress = ProgressDialog.show(MainActivityScreen.this,
                    "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            //if (myBT.getBtSocket() == null || !myBT.getConnected()) {
            if ( !myBT.getConnected()) {

                if (myBT.connect())
                    ConnectSuccess = true;
                else
                    ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                //Connection Failed. Is it a SPP Bluetooth? Try again.
                msg("Connection Failed");
                //finish();
            } else {
                //Connected.
                msg("Connected");
                //isBtConnected = true;
                myBT.setConnected(true);
                EnableUI();
                btnConnectDisconnect.setText("disconnect");
            }
            progress.dismiss();
        }
    }
}
