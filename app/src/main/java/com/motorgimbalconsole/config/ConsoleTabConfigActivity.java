package com.motorgimbalconsole.config;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.annotation.Nullable;
import androidx.annotation.Nullable;

/*import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;*/
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: Gimbal configuration. This is done with several tabs so that it can be manageable
 * on a small screen
 * @author: boris.dureau@neuf.fr
 **/
public class ConsoleTabConfigActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab1Fragment configPage1 = null;
    Tab2Fragment configPage2 = null;
    Tab3Fragment configPage3 = null;
    Tab4Fragment configPage4 = null;

    private Button btnDismiss, btnUpload;
    static ConsoleApplication myBT;
    private static GimbalConfigData GimbalCfg = null;

    private ProgressDialog progress;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        readConfig();
        setContentView(R.layout.activity_console_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        btnDismiss = (Button) findViewById(R.id.butDismiss);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();      //exit the  activity
            }
        });
        btnUpload = (Button) findViewById(R.id.butUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //send back the config to the altimeter and exit if successful
                sendConfig();
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());

        if (myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal")) {
            configPage1 = new Tab1Fragment();
            adapter.addFragment(configPage1, "TAB1");
        }
        configPage2 = new Tab2Fragment();
        configPage3 = new Tab3Fragment();
        configPage4 = new Tab4Fragment();

        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");
        adapter.addFragment(configPage4, "TAB4");

        linearDots=findViewById(R.id.idConsoleConfigLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr){
        dotsSlide =new TextView[nbr];
        linearDots.removeAllViews();

        for (int i=0; i< dotsSlide.length; i++){
            dotsSlide[i]=new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if(dotsSlide.length>0){
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }

    }

    ViewPager.OnPageChangeListener viewListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    static void readConfig() {
        // ask for config
        boolean success = false;
        if (myBT.getConnected()) {
            myBT.flush();
            myBT.clearInput();
            //msg("Retrieving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();
            myBT.write("b;".toString());
            myBT.flush();

            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if (myBT.getConnected()) {
            String myMessage = "";

            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                myBT.setDataReady(false);
                myMessage = myBT.ReadResult(10000);
            }

            if (myMessage.equals("start alticonfig end")) {
                try {
                    // getGimbalConfigData
                    GimbalCfg = myBT.getGimbalConfigData();
                    success = true;
                    //String conf;
                    //conf = GimbalCfg.getKpX() + "," + GimbalCfg.getKpY() +
                      //      "," + GimbalCfg.getKiY() + "," + GimbalCfg.getKdY();

                } catch (Exception e) {
                   // msg("pb ready data");
                }
            } else {
                //msg(getResources().getString(R.string.conf_msg1));
            }
        }
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private boolean sendConfig() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        long prevBaudRate = GimbalCfg.getConnectionSpeed();

        // check if the baud rate has changed
        if(myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal")) {
            if (configPage1.isViewCreated()) {
                GimbalCfg.setAxOffset(configPage1.getAxOffsetValue());
                GimbalCfg.setAyOffset(configPage1.getAyOffsetValue());
                GimbalCfg.setAzOffset(configPage1.getAzOffsetValue());
                GimbalCfg.setGxOffset(configPage1.getGxOffsetValue());
                GimbalCfg.setGyOffset(configPage1.getGyOffsetValue());
                GimbalCfg.setGzOffset(configPage1.getGzOffsetValue());
            }
        }
        if (configPage2.isViewCreated()) {

            GimbalCfg.setKpX(configPage2.getTxtKpXValue());
            GimbalCfg.setKiX(configPage2.getTxtKiXValue());
            GimbalCfg.setKdX(configPage2.getTxtKdXValue());
            GimbalCfg.setKpY(configPage2.getTxtKpYValue());
            GimbalCfg.setKiY(configPage2.getTxtKiYValue());
            GimbalCfg.setKdY(configPage2.getTxtKdYValue());
        }

        if (configPage3.isViewCreated()) {
            GimbalCfg.setBeepingFrequency(configPage3.getFreq());
            GimbalCfg.setUnits(configPage3.getDropdownUnits());
            GimbalCfg.setAltimeterResolution(configPage3.getAltimeterResolution());
            GimbalCfg.setEepromSize(configPage3.getEEpromSize());
            GimbalCfg.setEndRecordAltitude(configPage3.getEndRecordAltitude());
            GimbalCfg.setLiftOffDetect(configPage3.getLiftOffDetect());
            GimbalCfg.setAcceleroRange(configPage3.getAcceleroRange());
            GimbalCfg.setGyroRange(configPage3.getGyroRange());
            GimbalCfg.setRecordingTimeout(configPage3.getRecordingTimeout());
            GimbalCfg.setBatteryType(configPage3.getBatteryType());

        }
        if (configPage4.isViewCreated()) {
            GimbalCfg.setServoXMin(configPage4.getServoXMin());
            GimbalCfg.setServoXMax(configPage4.getServoXMax());
            GimbalCfg.setServoYMin(configPage4.getServoYMin());
            GimbalCfg.setServoYMax(configPage4.getServoYMax());
        }


        if (configPage3.isViewCreated()) {
            //GimbalCfg.setConnectionSpeed(configPage3.getBaudRate());
            if (GimbalCfg.getConnectionSpeed() != configPage3.getBaudRate()) {
                //final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //You are about to change the baud rate, are you sure you want to do it?
                builder.setMessage(getResources().getString(R.string.msg9))
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                GimbalCfg.setConnectionSpeed(configPage3.getBaudRate());
                                sendAltiCfgV2();
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            } else {
                sendAltiCfgV2();
                finish();
            }
        } else {
            sendAltiCfgV2();
            finish();
        }
        //sendAltiCfgV2();
       // finish();
        return true;
    }

    private void sendAltiCfg() {
        String gimbalCfgStr = "";

        gimbalCfgStr = "s," +
                GimbalCfg.getAxOffset() + "," +
                GimbalCfg.getAyOffset() + "," +
                GimbalCfg.getAzOffset() + "," +
                GimbalCfg.getGxOffset() + "," +
                GimbalCfg.getGyOffset() + "," +
                GimbalCfg.getGzOffset() + "," +
                //configPage2.getKpX()+","+
                (int) (GimbalCfg.getKpX() * 100) + "," +
                (int) (GimbalCfg.getKiX() * 100) + "," +
                (int) (GimbalCfg.getKdX() * 100) + "," +
                (int) (GimbalCfg.getKpY() * 100) + "," +
                (int) (GimbalCfg.getKiY() * 100) + "," +
                (int) (GimbalCfg.getKdY() * 100) + "," +
                GimbalCfg.getServoXMin() + "," +
                GimbalCfg.getServoXMax() + "," +
                GimbalCfg.getServoYMin() + "," +
                GimbalCfg.getServoYMax() + "," +
                GimbalCfg.getConnectionSpeed() + "," +
                GimbalCfg.getAltimeterResolution() + "," +
                GimbalCfg.getEepromSize() + "," +
                GimbalCfg.getUnits() + "," +
                GimbalCfg.getEndRecordAltitude() + "," +
                GimbalCfg.getBeepingFrequency() + "," +
                GimbalCfg.getLiftOffDetect() + "," +
                GimbalCfg.getGyroRange() + "," +
                GimbalCfg.getAcceleroRange() + "," +
                GimbalCfg.getRecordingTimeout()+ "," +
        GimbalCfg.getBatteryType();
        String cfg = gimbalCfgStr;
        cfg = cfg.replace("s","");
        cfg = cfg.replace(",","");


        gimbalCfgStr = gimbalCfgStr + ","+ generateCheckSum(cfg) +";";

       // if (myBT.getConnected())

        myBT.setDataReady(false);
        myBT.flush();
        myBT.clearInput();
        //switch off the main loop before sending the config
        myBT.write("m0;".toString());

        //wait for the result to come back
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        myMessage = myBT.ReadResult(3000);

        myBT.flush();
        myBT.clearInput();
        myBT.setDataReady(false);
        //send back the config
        myBT.write(gimbalCfgStr.toString());
        myBT.flush();
        //get the results
        //wait for the result to come back
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }

        //myMessage = "";
        //long timeOut = 10000;
        //long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(3000);
        if (myMessage.equals("OK")) {
            msg("Sent OK:" + gimbalCfgStr.toString());
        }
        else {
          //  msg(myMessage);
        }
        if (myMessage.equals("KO")) {
            msg(getResources().getString(R.string.conf_msg2));
        }
        Log.d("configboris", gimbalCfgStr.toString());

        myBT.setDataReady(false);
        myBT.flush();
        myBT.clearInput();
        //switch on the main loop before sending the config
        myBT.write("m1;".toString());


        //wait for the result to come back
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        myMessage = "";
        myMessage = myBT.ReadResult(3000);
    }

    private void sendAltiCfgV2() {

        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
        }

        //String gimbalCfgStr = "";

        //gimbalCfgStr = "s," +
        //        GimbalCfg.getAxOffset() + "," +
        SendParam("p,1,"+ GimbalCfg.getAxOffset());
        //        GimbalCfg.getAyOffset() + "," +
        SendParam("p,2,"+ GimbalCfg.getAyOffset());
        //        GimbalCfg.getAzOffset() + "," +
        SendParam("p,3,"+ GimbalCfg.getAzOffset());
        //        GimbalCfg.getGxOffset() + "," +
        SendParam("p,4,"+ GimbalCfg.getGxOffset());
        //        GimbalCfg.getGyOffset() + "," +
        SendParam("p,5,"+ GimbalCfg.getGyOffset());
        //         GimbalCfg.getGzOffset() + "," +
        SendParam("p,6,"+ GimbalCfg.getGzOffset());
        //        (int) (GimbalCfg.getKpX() * 100) + "," +
        SendParam("p,7,"+ (int) (GimbalCfg.getKpX() * 100) );
        //        (int) (GimbalCfg.getKiX() * 100) + "," +
        SendParam("p,8,"+ (int) (GimbalCfg.getKiX() * 100) );
        //        (int) (GimbalCfg.getKdX() * 100) + "," +
        SendParam("p,9,"+ (int) (GimbalCfg.getKdX() * 100) );
        //        (int) (GimbalCfg.getKpY() * 100) + "," +
        SendParam("p,10,"+ (int) (GimbalCfg.getKpY() * 100) );
        //        (int) (GimbalCfg.getKiY() * 100) + "," +
        SendParam("p,11,"+ (int) (GimbalCfg.getKiY() * 100) );
        //        (int) (GimbalCfg.getKdY() * 100) + "," +
        SendParam("p,12,"+ (int) (GimbalCfg.getKdY() * 100) );
        //        GimbalCfg.getServoXMin() + "," +
        SendParam("p,13,"+ GimbalCfg.getServoXMin());
        //        GimbalCfg.getServoXMax() + "," +
        SendParam("p,14,"+ GimbalCfg.getServoXMax());
        //        GimbalCfg.getServoYMin() + "," +
        SendParam("p,15,"+ GimbalCfg.getServoYMin());
        //        GimbalCfg.getServoYMax() + "," +
        SendParam("p,16,"+ GimbalCfg.getServoYMax());
        //        GimbalCfg.getConnectionSpeed() + "," +
        SendParam("p,17,"+ GimbalCfg.getConnectionSpeed());
        //        GimbalCfg.getAltimeterResolution() + "," +
        SendParam("p,18,"+ GimbalCfg.getAltimeterResolution());
        //        GimbalCfg.getEepromSize() + "," +
        SendParam("p,19,"+ GimbalCfg.getEepromSize());
        //        GimbalCfg.getUnits() + "," +
        SendParam("p,20,"+ GimbalCfg.getUnits());
        //        GimbalCfg.getEndRecordAltitude() + "," +
        SendParam("p,21,"+ GimbalCfg.getEndRecordAltitude());
        //        GimbalCfg.getBeepingFrequency() + "," +
        SendParam("p,22,"+ GimbalCfg.getBeepingFrequency());
        //        GimbalCfg.getLiftOffDetect() + "," +
        SendParam("p,23,"+ GimbalCfg.getLiftOffDetect());
        //        GimbalCfg.getGyroRange() + "," +
        SendParam("p,24,"+ GimbalCfg.getGyroRange());
        //        GimbalCfg.getAcceleroRange() + "," +
        SendParam("p,25,"+ GimbalCfg.getAcceleroRange());
        //        GimbalCfg.getRecordingTimeout()+ "," +
        SendParam("p,26,"+ GimbalCfg.getRecordingTimeout());
        //        GimbalCfg.getBatteryType();
        SendParam("p,27,"+ GimbalCfg.getBatteryType());

        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //Write the config structure
            myBT.write("q;".toString());
            Log.d("conftab", "write config");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }
    }

    private void SendParam (String altiCfgStr) {
        String cfg = altiCfgStr;
        cfg = cfg.replace("p", "");
        cfg = cfg.replace(",", "");
        Log.d("conftab", cfg.toString());

        altiCfgStr = altiCfgStr + "," + generateCheckSum(cfg) + ";";


        if (myBT.getConnected()) {

            String myMessage = "";

            myBT.flush();
            myBT.clearInput();
            myBT.setDataReady(false);
            //msg("Sent :" + altiCfgStr.toString());
            //send back the config
            myBT.write(altiCfgStr.toString());
            Log.d("conftab", altiCfgStr.toString());
            myBT.flush();
            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                //msg("Sent OK:" + altiCfgStr.toString());
                Log.d("conftab", "config sent succesfully");

            } else {
                //  msg(myMessage);
                Log.d("conftab", "config not sent succesfully");
                Log.d("conftab", myMessage);
            }
            if (myMessage.equals("KO")) {
                //   msg(getResources().getString(R.string.msg2));
            }
        }
    }
    public static Integer generateCheckSum(String value)  {

        byte[] data = value.getBytes();
        long checksum = 0L;

        for( byte b : data )  {
            checksum += b;
        }

        checksum = checksum % 256;

        return new Long( checksum ).intValue();

    }
    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1Fragment extends Fragment {
        private static final String TAG = "Tab1Fragment";
        private boolean ViewCreated = false;
        private EditText txtViewAxOffset, txtViewAyOffset, txtViewAzOffset;
        private EditText txtViewGxOffset, txtViewGyOffset, txtViewGzOffset;
        private Button btnCalibrate;


        public void setAxOffsetValue(long value) {
            this.txtViewAxOffset.setText(String.valueOf(value));
        }

        public long getAxOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewAxOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setAyOffsetValue(long value) {
            this.txtViewAyOffset.setText(String.valueOf(value));
        }

        public long getAyOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewAyOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setAzOffsetValue(long value) {
            this.txtViewAzOffset.setText(String.valueOf(value));
        }

        public long getAzOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewAzOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setGxOffsetValue(long value) {
            this.txtViewGxOffset.setText(String.valueOf(value));
        }

        public long getGxOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewGxOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setGyOffsetValue(long value) {
            this.txtViewGyOffset.setText(String.valueOf(value));
        }

        public long getGyOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewGyOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setGzOffsetValue(long value) {
            this.txtViewGzOffset.setText(String.valueOf(value));
        }

        public long getGzOffsetValue() {
            long ret;
            try {
                ret = Long.parseLong(this.txtViewGzOffset.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart1_fragment, container, false);
            txtViewAxOffset = (EditText) view.findViewById(R.id.editTxtAxOffset);
            txtViewAyOffset = (EditText) view.findViewById(R.id.editTxtAyOffset);
            txtViewAzOffset = (EditText) view.findViewById(R.id.editTxtAzOffset);

            txtViewGxOffset = (EditText) view.findViewById(R.id.editTxtGxOffset);
            txtViewGyOffset = (EditText) view.findViewById(R.id.editTxtGyOffset);
            txtViewGzOffset = (EditText) view.findViewById(R.id.editTxtGzOffset);
            if (GimbalCfg != null) {
                setAxOffsetValue(GimbalCfg.getAxOffset());
                setAyOffsetValue(GimbalCfg.getAyOffset());
                setAzOffsetValue(GimbalCfg.getAzOffset());
                setGxOffsetValue(GimbalCfg.getGxOffset());
                setGyOffsetValue(GimbalCfg.getGyOffset());
                setGzOffsetValue(GimbalCfg.getGzOffset());
            }
            btnCalibrate = (Button) view.findViewById(R.id.butCalibrate);

            btnCalibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    new Calibration().execute();
                }
            });
            ViewCreated = true;
            return view;
        }
        // calibration
        private class Calibration extends AsyncTask<Void, Void, Void>  // UI thread
        {
            private AlertDialog.Builder builder = null;
            private AlertDialog alert;
            private Boolean canceled = false;

            @Override
            protected void onPreExecute() {
                //"Calibration in progress..."
                //"Please wait!!!"
                //this.getActivity()
            builder = new AlertDialog.Builder(Tab1Fragment.this.getContext());

            builder.setMessage("Calibration...")
                    .setTitle("Calibration")
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            myBT.setExit(true);
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

                myBT.flush();
                myBT.clearInput();
                myBT.write("c;".toString());
                //wait for ok and put the result back
                String myMessage = "";

                myMessage = myBT.ReadResult(3000);
                if (myMessage.equals("OK")) {
                    readConfig();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
            {
                super.onPostExecute(result);
                if (!canceled) {
                    readConfig();
                    setAxOffsetValue(GimbalCfg.getAxOffset());
                    setAyOffsetValue(GimbalCfg.getAyOffset());
                    setAzOffsetValue(GimbalCfg.getAzOffset());
                    setGxOffsetValue(GimbalCfg.getGxOffset());
                    setGyOffsetValue(GimbalCfg.getGyOffset());
                    setGzOffsetValue(GimbalCfg.getGzOffset());
                    alert.dismiss();
                }

            }
        }
    }

    public static class Tab2Fragment extends Fragment {
        private static final String TAG = "Tab2Fragment";
        private boolean ViewCreated = false;
        private EditText editTxtKpX, editTxtKiX, editTxtKdX;
        private EditText editTxtKpY, editTxtKiY, editTxtKdY;

        public void setTxtKpXValue(double value) {
            this.editTxtKpX.setText(Double.toString(value));
        }

        public double getTxtKpXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKpX.getText().toString());


            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;
        }

        public void setTxtKiXValue(double value) {
            this.editTxtKiX.setText(Double.toString(value));
        }

        public double getTxtKiXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKiX.getText().toString());
            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;
        }

        public void setTxtKdXValue(double value) {
            this.editTxtKdX.setText(Double.toString(value));
        }

        public double getTxtKdXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKdX.getText().toString());
            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;

        }

        public void setTxtKpYValue(double value) {
            this.editTxtKpY.setText(Double.toString(value));
        }

        public double getTxtKpYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKpY.getText().toString());
            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;
        }

        public void setTxtKiYValue(double value) {
            //this.editTxtKiY.setText(String.format("%.2f",value));
            this.editTxtKiY.setText(Double.toString(value));
        }

        public double getTxtKiYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKiY.getText().toString());
            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;
        }

        public void setTxtKdYValue(double value) {
            this.editTxtKdY.setText(Double.toString(value));
        }

        public double getTxtKdYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKdY.getText().toString());
            } catch (Exception e) {
                ret = 0.0;
            }
            return ret;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart2_fragment, container, false);

            editTxtKpX = (EditText) view.findViewById(R.id.editTxtKpX);
            editTxtKiX = (EditText) view.findViewById(R.id.editTxtKiX);
            editTxtKdX = (EditText) view.findViewById(R.id.editTxtKdX);

            editTxtKpY = (EditText) view.findViewById(R.id.editTxtKpY);
            editTxtKiY = (EditText) view.findViewById(R.id.editTxtKiY);
            editTxtKdY = (EditText) view.findViewById(R.id.editTxtKdY);

            if (GimbalCfg != null) {
                setTxtKpXValue(GimbalCfg.getKpX());
                setTxtKiXValue(GimbalCfg.getKiX());
                setTxtKdXValue(GimbalCfg.getKdX());

                setTxtKpYValue(GimbalCfg.getKpY());
                setTxtKiYValue(GimbalCfg.getKiY());
                setTxtKdYValue(GimbalCfg.getKdY());
                /*editTxtKpX.setText(String.valueOf(GimbalCfg.getKpX()));
                editTxtKiX.setText(String.valueOf(GimbalCfg.getKiX()));
                editTxtKdX.setText(String.valueOf(GimbalCfg.getKdX()));

                editTxtKpY.setText(String.valueOf(GimbalCfg.getKpY()));
                editTxtKiY.setText(String.valueOf(GimbalCfg.getKiY()));
                editTxtKdY.setText(String.valueOf(GimbalCfg.getKdY()));*/
            }

            ViewCreated = true;
            return view;
        }
    }

    public static class Tab3Fragment extends Fragment {
        private static final String TAG = "Tab3Fragment";
        private boolean ViewCreated = false;
        private String[] itemsBaudRate;
        private String[] itemsAltimeterResolution;
        private String[] itemsEEpromSize;
        private String[] itemsLaunchDetect;
        private String[] itemsGyroRange;
        private String[] itemsAcceleroRange;
        private String[] itemsBatteryType;

        private Spinner dropdownBaudRate;
        private Spinner dropdownAltimeterResolution, dropdownEEpromSize, dropdownLaunchDetect;
        private Spinner dropdownBatteryType;
        private EditText EndRecordAltitude;
        private Spinner dropdownUnits;
        private TextView altiName;
        private EditText Freq,RecordingTimeout;
        private Spinner dropdownGyroRange, dropdownAcceleroRange;
        private TextView txtViewGyroRange, txtViewAcceleroRange;

        //txtAltiNameValue
        //spinnerUnit
        //editTxtBipFreq
        //spinnerBaudRate
        //spinnerAltimeterResolution
        //spinnerEEpromSize
        //txtViewEndRecordAltitude
        public int getFreq() {
            int ret;
            try {
                ret = Integer.parseInt(this.Freq.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;

        }

        public void setFreq(int freq) {
            Freq.setText(freq);
        }

        public void setAltiName(String altiName) {
            this.altiName.setText(altiName);
        }

        public String getAltiName() {
            return (String) this.altiName.getText();
        }

        public int getDropdownUnits() {
            return (int) this.dropdownUnits.getSelectedItemId();
        }

        public void setDropdownUnits(int Units) {
            this.dropdownUnits.setSelection(Units);
        }


        public int getAltimeterResolution() {
            return (int) this.dropdownAltimeterResolution.getSelectedItemId();
        }

        public void setAltimeterResolution(int AltimeterResolution) {
            this.dropdownAltimeterResolution.setSelection(AltimeterResolution);
        }

        public int getEEpromSize() {
            int ret;
            try {
                ret = Integer.parseInt(itemsEEpromSize[(int) dropdownEEpromSize.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setEEpromSize(int EEpromSize) {
            this.dropdownEEpromSize.setSelection(GimbalCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
        }

        public long getBaudRate() {
            long ret;
            try {
                ret = Long.parseLong(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setBaudRate(long BaudRate) {
            this.dropdownBaudRate.setSelection(GimbalCfg.arrayIndex(itemsBaudRate, String.valueOf(BaudRate)));
        }

        public int getEndRecordAltitude() {
            int ret;
            try {
                ret = Integer.parseInt(this.EndRecordAltitude.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setEndRecordAltitude(int EndRecordAltitude) {
            this.EndRecordAltitude.setText(String.valueOf(EndRecordAltitude));
        }

        public int getLiftOffDetect() {
            int ret;
            try {
                ret = (int) dropdownLaunchDetect.getSelectedItemId();
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getGyroRange() {
            int ret;
            try {
                ret = (int) dropdownGyroRange.getSelectedItemId();
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getAcceleroRange() {
            int ret;
            try {
                ret = (int) dropdownAcceleroRange.getSelectedItemId();
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public int getBatteryType() {
            return (int) this.dropdownBatteryType.getSelectedItemId();
        }

        public void setBatteryType(int BatteryType) {
            dropdownBatteryType.setSelection(BatteryType);
        }

        public int getRecordingTimeout() {
            int ret;
            try {
                ret = Integer.parseInt(this.RecordingTimeout.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setRecordingTimeout(int RecordingTimeout) {
            this.RecordingTimeout.setText(String.valueOf(RecordingTimeout));
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart3_fragment, container, false);

            //units
            dropdownUnits = (Spinner) view.findViewById(R.id.spinnerUnit);
            //"Meters", "Feet"
            String[] items2 = new String[]{getResources().getString(R.string.unit_meter),
                    getResources().getString(R.string.unit_feet)};
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items2);
            dropdownUnits.setAdapter(adapter2);

            //Altimeter name
            altiName = (TextView) view.findViewById(R.id.txtAltiNameValue);
            //here you can set the beep frequency
            Freq = (EditText) view.findViewById(R.id.editTxtBipFreq);

            //baud rate
            dropdownBaudRate = (Spinner) view.findViewById(R.id.spinnerBaudRate);
            itemsBaudRate = new String[]{"300",
                    "1200",
                    "2400",
                    "4800",
                    "9600",
                    "14400",
                    "19200",
                    "28800",
                    "38400",
                    "57600",
                    "115200",
                    "230400"};
            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
            dropdownBaudRate.setAdapter(adapterBaudRate);
            // altimeter resolution
            dropdownAltimeterResolution = (Spinner) view.findViewById(R.id.spinnerAltimeterResolution);
            itemsAltimeterResolution = new String[]{"ULTRALOWPOWER", "STANDARD", "HIGHRES", "ULTRAHIGHRES"};
            ArrayAdapter<String> adapterAltimeterResolution = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsAltimeterResolution);
            dropdownAltimeterResolution.setAdapter(adapterAltimeterResolution);

            //Altimeter external eeprom size
            dropdownEEpromSize = (Spinner) view.findViewById(R.id.spinnerEEpromSize);
            itemsEEpromSize = new String[]{"32", "64", "128", "256", "512", "1024"};
            ArrayAdapter<String> adapterEEpromSize = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
            dropdownEEpromSize.setAdapter(adapterEEpromSize);
            // nbr of meters to stop recording altitude
            EndRecordAltitude = (EditText) view.findViewById(R.id.editTxtEndRecordAltitude);

            //spinnerLaunchDetect
            dropdownLaunchDetect = (Spinner) view.findViewById(R.id.spinnerLaunchDetect);
            //"baro", "Accel"
            itemsLaunchDetect = new String[]{getResources().getString(R.string.baro), getResources().getString(R.string.accel)};
            ArrayAdapter<String> adapterLaunchDetect = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsLaunchDetect);
            dropdownLaunchDetect.setAdapter(adapterLaunchDetect);

            //spinnerGyro Range
            dropdownGyroRange = (Spinner) view.findViewById(R.id.spinnerGyroRange);
            itemsGyroRange = new String[]{"GYRO_FS_250" ,"GYRO_FS_500","GYRO_FS_1000", "GYRO_FS_2000"};
            ArrayAdapter<String> adapterGyroRange = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsGyroRange);
            dropdownGyroRange.setAdapter(adapterGyroRange);

            //spinnerAccelero Range
            dropdownAcceleroRange = (Spinner) view.findViewById(R.id.spinnerAcceleroRange);
            itemsAcceleroRange = new String[]{"ACCEL_FS_2" ,"ACCEL_FS_4","ACCEL_FS_8", "ACCEL_FS_16"};
            ArrayAdapter<String> adapterAcceleroRange = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsAcceleroRange);
            dropdownAcceleroRange.setAdapter(adapterAcceleroRange);

            txtViewGyroRange = (TextView) view.findViewById(R.id.txtViewGyroRange);

            txtViewAcceleroRange= (TextView) view.findViewById(R.id.txtViewAcceleroRange);
            dropdownBatteryType = (Spinner) view.findViewById(R.id.spinnerBatteryType);
            //"Unknown",
            itemsBatteryType = new String[]{getResources().getString(R.string.config_unknown),
                    "2S (7.4 Volts)", "9 Volts", "3S (11.1 Volts)"};
            ArrayAdapter<String> adapterBatteryType = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsBatteryType);
            dropdownBatteryType.setAdapter(adapterBatteryType);
            //Max recording time in seconds
            RecordingTimeout =(EditText) view.findViewById(R.id.editTxtRecordingTimeOut);

            if (GimbalCfg != null) {
                setBaudRate(GimbalCfg.getConnectionSpeed());
                //dropdownBaudRate.setSelection(GimbalCfg.arrayIndex(itemsBaudRate,String.valueOf(GimbalCfg.getConnectionSpeed())));
                dropdownAltimeterResolution.setSelection(GimbalCfg.getAltimeterResolution());
                dropdownEEpromSize.setSelection(GimbalCfg.arrayIndex(itemsEEpromSize, String.valueOf(GimbalCfg.getEepromSize())));
                EndRecordAltitude.setText(String.valueOf(GimbalCfg.getEndRecordAltitude()));

                altiName.setText(GimbalCfg.getAltimeterName() + " ver: " +
                        GimbalCfg.getAltiMajorVersion() + "." + GimbalCfg.getAltiMinorVersion());


                dropdownUnits.setSelection(GimbalCfg.getUnits());
                Freq.setText(String.valueOf(GimbalCfg.getBeepingFrequency()));
                dropdownLaunchDetect.setSelection(GimbalCfg.getLiftOffDetect());
                dropdownGyroRange.setSelection(GimbalCfg.getGyroRange());
                dropdownAcceleroRange.setSelection(GimbalCfg.getAcceleroRange());
                if (myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal")) {
                    dropdownGyroRange.setVisibility(View.VISIBLE);
                    dropdownAcceleroRange.setVisibility(View.VISIBLE);
                    txtViewGyroRange.setVisibility(View.VISIBLE);
                    txtViewAcceleroRange.setVisibility(View.VISIBLE);
                }
                else {
                    dropdownGyroRange.setVisibility(View.INVISIBLE);
                    dropdownAcceleroRange.setVisibility(View.INVISIBLE);
                    txtViewGyroRange.setVisibility(View.INVISIBLE);
                    txtViewAcceleroRange.setVisibility(View.INVISIBLE);
                }
                dropdownBatteryType.setSelection(GimbalCfg.getBatteryType());
                RecordingTimeout.setText(String.valueOf(GimbalCfg.getRecordingTimeout()));
            }
            ViewCreated = true;
            return view;
        }
    }

    public static class Tab4Fragment extends Fragment {
        private static final String TAG = "Tab4Fragment";
        private boolean ViewCreated = false;

        private EditText editTxtViewServoXMin, editTxtViewServoXMax, editTxtViewServoYMin, editTxtViewServoYMax;


        public void setServoXMin(int value) {
            this.editTxtViewServoXMin.setText(String.valueOf(value));
        }

        public int getServoXMin() {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoXMin.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServoXMax(int value) {
            this.editTxtViewServoXMax.setText(String.valueOf(value));
        }

        public int getServoXMax() {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoXMax.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServoYMin(int value) {
            this.editTxtViewServoYMin.setText(String.valueOf(value));
        }

        public int getServoYMin() {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoYMin.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServoYMax(int value) {
            this.editTxtViewServoYMax.setText(String.valueOf(value));
        }

        public int getServoYMax() {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoYMax.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public boolean isViewCreated() {
            return ViewCreated;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart4_fragment, container, false);

            editTxtViewServoXMin = (EditText) view.findViewById(R.id.editTxtServoXMin);
            editTxtViewServoXMax = (EditText) view.findViewById(R.id.editTxtServoXMax);
            editTxtViewServoYMin = (EditText) view.findViewById(R.id.editTxtServoYMin);
            editTxtViewServoYMax = (EditText) view.findViewById(R.id.editTxtServoYMax);

            if (GimbalCfg != null) {
                setServoXMin(GimbalCfg.getServoXMin());
                setServoXMax(GimbalCfg.getServoXMax());
                setServoYMax(GimbalCfg.getServoYMax());
                setServoYMin(GimbalCfg.getServoYMin());
            }
            ViewCreated = true;
            return view;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(ConsoleTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_gimbal_config");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(ConsoleTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}