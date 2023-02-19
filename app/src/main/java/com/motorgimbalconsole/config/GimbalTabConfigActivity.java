package com.motorgimbalconsole.config;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.ShareHandler;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab1Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab2Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab3Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab4Fragment;
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
public class GimbalTabConfigActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    GimbalConfigTab1Fragment configPage1 = null;
    GimbalConfigTab2Fragment configPage2 = null;
    GimbalConfigTab3Fragment configPage3 = null;
    GimbalConfigTab4Fragment configPage4 = null;

    private Button btnDismiss, btnUpload;
    private static ConsoleApplication myBT;
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
            configPage1 = new GimbalConfigTab1Fragment(myBT,GimbalCfg);
            adapter.addFragment(configPage1, "TAB1");
        }
        configPage2 = new GimbalConfigTab2Fragment(myBT,GimbalCfg);
        configPage3 = new GimbalConfigTab3Fragment(myBT,GimbalCfg);
        configPage4 = new GimbalConfigTab4Fragment(myBT,GimbalCfg);

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

    public static void readConfig() {
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

        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }

        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(GimbalTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_gimbal_config");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(GimbalTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}