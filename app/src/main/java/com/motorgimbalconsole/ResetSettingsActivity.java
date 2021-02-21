package com.motorgimbalconsole;
/**
 *   @description: Allow the user to reset the altimeter setting to factory default
 *   as well as clearing the flight list
 *   @author: boris.dureau@neuf.fr
 **/
import android.app.AlertDialog;
import android.content.DialogInterface;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class ResetSettingsActivity extends AppCompatActivity {

    Button btnClearAltiConfig, btnClearFlights, btnDismiss;
    ConsoleApplication myBT ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        setContentView(R.layout.activity_reset_settings);


        btnDismiss = (Button)findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the application configuration activity
            }
        });

        btnClearAltiConfig = (Button)findViewById(R.id.butRestoreAltiCfg);
        btnClearAltiConfig.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)      {
                clearAltiConfig();
            }
        });
        btnClearFlights = (Button)findViewById(R.id.butClearFlights);
        btnClearFlights.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                clearFlights();

            }
        });
        if(myBT.getGimbalConfigData().getAltimeterName().equals("AltiServo") )
            btnClearFlights.setVisibility(View.INVISIBLE);
        else
            btnClearFlights.setVisibility(View.VISIBLE);
    }

    public void clearFlights() {
     final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to erase all flight data, are you sure you want to do it?
     builder.setMessage(getResources().getString(R.string.reset_msg1))
             .setCancelable(false)
             .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                 public void onClick(final DialogInterface dialog, final int id) {
                     dialog.cancel();
                     //clear altimeter config
                     if(myBT.getConnected())
                      //   try  {
                             //erase the config
                             myBT.write("e;\n".toString());
                             myBT.flush();
                         //}
                        /* catch (IOException e) {

                         }*/

                 }
             })
             .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                 public void onClick(final DialogInterface dialog, final int id) {
                     dialog.cancel();

                 }
             });
     final AlertDialog alert = builder.create();
     alert.show();
    }
    public void clearAltiConfig() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //You are about to reset your altimeter config, are you sure you want to do it?
        builder.setMessage(getResources().getString(R.string.reset_msg2))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        //clear altimeter config
                        if(myBT.getConnected())
                            //try  {
                                //erase the config
                                myBT.write("d;\n".toString());
                                myBT.flush();
                           /* }
                            catch (IOException e) {

                            }*/
                    }
                })
                .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
