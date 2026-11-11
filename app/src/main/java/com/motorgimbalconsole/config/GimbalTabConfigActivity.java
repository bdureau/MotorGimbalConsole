package com.motorgimbalconsole.config;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.text.TextUtils;
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
import com.motorgimbalconsole.utils.BackgroundTask;
import com.motorgimbalconsole.utils.PagerFragments;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab1Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab2Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab3Fragment;
import com.motorgimbalconsole.config.GimbalConfig.GimbalConfigTab4Fragment;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;

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
    // Not static any more: a static reference to the activity state outlived the screen
    private ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;
    // "Please wait" dialog shown while the board is read or written in the background
    private AlertDialog progressDialog = null;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void attachBaseContext(Context base) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = base.getSharedPreferences("GimbalConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(base, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        // Configuration already read when the board was connected; refreshed below
        GimbalCfg = myBT.getGimbalConfigData();
        setContentView(R.layout.activity_gimbal_tab_config);

        mViewPager = (ViewPager) findViewById(R.id.container);
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

        if (savedInstanceState == null && myBT.getConnected()) {
            // First opening: re-read the board configuration in the background (it used to be
            // read on the UI thread with a busy-wait, freezing the screen), then build the tabs
            loadConfigThenShowTabs();
        } else {
            // Re-creation (rotation...): the restored tabs keep the values being edited
            setupViewPager(mViewPager);
        }
    }

    /**
     * Reads the configuration from the board in the background, then builds the tabs.
     * The upload button stays disabled until the tabs exist.
     */
    private void loadConfigThenShowTabs() {
        btnUpload.setEnabled(false);
        new BackgroundTask() {
            @Override
            protected void onPreExecute() {
                showProgress();
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (!readConfig()) {
                    Log.w("conftab", "Unable to refresh the configuration, using the cached one");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                hideProgress();
                if (isFinishing() || isDestroyed()) return; // screen closed meanwhile
                GimbalCfg = myBT.getGimbalConfigData();
                setupViewPager(mViewPager);
                btnUpload.setEnabled(true);
            }
        }.execute();
    }

    /** Shows the non cancelable "Please wait" dialog. */
    private void showProgress() {
        hideProgress();
        progressDialog = new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.MS_msg2))
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    /** Hides the "Please wait" dialog if it is displayed. */
    private void hideProgress() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // window already gone (screen closed)
            }
            progressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        hideProgress();
        super.onDestroy();
    }

    /**
     * Builds the tabs, re-using the fragments restored by Android after a re-creation
     * (see PagerFragments) so that sendConfig() reads the values of the tabs displayed.
     */
    private void setupViewPager(ViewPager viewPager) {
        FragmentManager fm = getSupportFragmentManager();
        adapter = new SectionsPageAdapter(fm);

        if (myBT.getGimbalConfigData().getAltimeterName().equals("RocketMotorGimbal")) {
            configPage1 = PagerFragments.restoredOr(fm, GimbalConfigTab1Fragment.class, GimbalConfigTab1Fragment::new);
            adapter.addFragment(configPage1, "TAB1");
        }
        configPage2 = PagerFragments.restoredOr(fm, GimbalConfigTab2Fragment.class, GimbalConfigTab2Fragment::new);
        configPage3 = PagerFragments.restoredOr(fm, GimbalConfigTab3Fragment.class, GimbalConfigTab3Fragment::new);
        configPage4 = PagerFragments.restoredOr(fm, GimbalConfigTab4Fragment.class, GimbalConfigTab4Fragment::new);

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

    /**
     * Reads the configuration from the board ("b;" then "$alticonfig" sentence). The values
     * are stored in the shared GimbalConfigData of the application.
     *
     * Blocking: must be called from a background thread (BackgroundTask). It used to start
     * with a busy-wait loop (while (available() <= 0);) that froze the UI thread forever if the
     * board did not answer; ReadResult() already waits for the answer, with a timeout.
     *
     * @return true if a complete configuration was received
     */
    public static boolean readConfig() {
        ConsoleApplication app = ConsoleApplication.getInstance();
        if (app == null || !app.getConnected()) return false;

        app.setDataReady(false);
        app.flush();
        app.clearInput();
        app.write("b;");
        app.flush();

        String myMessage = app.ReadResult(3000);
        if (myMessage.equals("OK")) {
            // acknowledgement first, then the configuration itself
            app.setDataReady(false);
            myMessage = app.ReadResult(10000);
        }
        return myMessage.equals("start alticonfig end");
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
            GimbalCfg.setTelemetryType(configPage3.getTelemetryType());
        }
        if (configPage4.isViewCreated()) {
            GimbalCfg.setServoXMin(configPage4.getServoXMin());
            GimbalCfg.setServoXMax(configPage4.getServoXMax());
            GimbalCfg.setServoYMin(configPage4.getServoYMin());
            GimbalCfg.setServoYMax(configPage4.getServoYMax());
        }


        // Refuse to send an aberrant configuration (empty field read as 0, inverted servo
        // limits, negative PID gain...): the board would store it in EEPROM for the next flight
        if (!checkConfigBeforeUpload()) {
            return false;
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
                                uploadConfigThenFinish();
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
                uploadConfigThenFinish();
            }
        } else {
            uploadConfigThenFinish();
        }
        //sendAltiCfgV2();
        // finish();
        return true;
    }

    /**
     * Checks the values about to be sent: empty or non numeric fields first, then the ranges
     * of GimbalConfigValidator. Displays every problem in one dialog.
     *
     * @return true if the configuration can be sent
     */
    private boolean checkConfigBeforeUpload() {
        boolean mpuBoard = GimbalCfg.getAltimeterName().equals("RocketMotorGimbal");
        List<String> invalidFields = new ArrayList<>();
        if (mpuBoard && configPage1 != null) invalidFields.addAll(configPage1.getInvalidFields());
        invalidFields.addAll(configPage2.getInvalidFields());
        invalidFields.addAll(configPage3.getInvalidFields());
        invalidFields.addAll(configPage4.getInvalidFields());

        StringBuilder message = new StringBuilder();
        if (!invalidFields.isEmpty()) {
            message.append(getString(R.string.cfg_err_empty_fields, TextUtils.join(", ", invalidFields)));
        } else {
            // Ranges are only meaningful once every field holds a number
            for (GimbalConfigValidator.Problem problem : GimbalConfigValidator.validate(GimbalCfg, mpuBoard)) {
                if (message.length() > 0) message.append("\n\n");
                message.append(describe(problem));
            }
        }
        if (message.length() == 0) return true;

        Log.w("conftab", "Configuration refused: " + message);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cfg_err_title))
                .setMessage(message.toString())
                .setPositiveButton(getString(R.string.save_curve_ok), null)
                .show();
        return false;
    }

    /**
     * @param problem problem found by GimbalConfigValidator
     * @return the translated explanation displayed to the user
     */
    private String describe(GimbalConfigValidator.Problem problem) {
        switch (problem.rule) {
            case SERVO_LIMITS:
                return getString(R.string.cfg_err_servo, problem.field, problem.values[0], problem.values[1]);
            case PID_GAIN:
                return getString(R.string.cfg_err_pid, problem.field, problem.values[0]);
            case SENSOR_OFFSET:
                return getString(R.string.cfg_err_offset, problem.field, problem.values[0]);
            case SENSOR_RANGE:
                return getString(R.string.cfg_err_sensor_range, problem.field);
            case BEEP_FREQUENCY:
                return getString(R.string.cfg_err_beep, problem.values[0]);
            case END_RECORD_ALTITUDE:
                return getString(R.string.cfg_err_end_record, problem.values[0]);
            case RECORDING_TIMEOUT:
            default:
                return getString(R.string.cfg_err_timeout, problem.values[0]);
        }
    }

    /**
     * Sends the configuration to the board in the background (28 parameters, each waiting for
     * the board acknowledgement: this used to freeze the UI thread for several seconds), then
     * closes the screen. If a parameter was refused, the screen stays open with a message so
     * that the user can try again.
     */
    private void uploadConfigThenFinish() {
        btnUpload.setEnabled(false);
        final boolean[] success = {false};
        new BackgroundTask() {
            @Override
            protected void onPreExecute() {
                showProgress();
            }

            @Override
            protected Void doInBackground(Void... params) {
                success[0] = sendAltiCfgV2();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                hideProgress();
                if (isFinishing() || isDestroyed()) return;
                if (success[0]) {
                    finish();
                } else {
                    btnUpload.setEnabled(true);
                    msg(getResources().getString(R.string.conf_msg2));
                }
            }
        }.execute();
    }

    /**
     * Writes the configuration to the board: main loop off, every parameter, save, main loop on.
     * Blocking: called from a background thread.
     *
     * @return true if the board acknowledged every parameter
     */
    private boolean sendAltiCfgV2() {
        if (!myBT.getConnected()) return false;
        boolean allSent = true;

        if (myBT.getConnected()) {
            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch off the main loop before sending the config
            myBT.write("m0;".toString());
            Log.d("conftab", "switch off main loop");
            //wait for the result to come back
            // ReadResult() waits for the answer itself (bounded by its timeout)
            String myMessage = "";
            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                Log.d("conftab", "switch off main loop ok");
            }
        }

        allSent &= SendParam("p,1,"+ GimbalCfg.getAxOffset());
        allSent &= SendParam("p,2,"+ GimbalCfg.getAyOffset());
        allSent &= SendParam("p,3,"+ GimbalCfg.getAzOffset());
        allSent &= SendParam("p,4,"+ GimbalCfg.getGxOffset());
        allSent &= SendParam("p,5,"+ GimbalCfg.getGyOffset());
        allSent &= SendParam("p,6,"+ GimbalCfg.getGzOffset());
        // PID gains are sent as hundredths. Math.round is required: a plain (int) cast truncates
        // binary floating point errors (0.29 * 100 = 28.999...), so every read/save cycle
        // lowered the gain by 0.01.
        allSent &= SendParam("p,7,"+ Math.round(GimbalCfg.getKpX() * 100) );
        allSent &= SendParam("p,8,"+ Math.round(GimbalCfg.getKiX() * 100) );
        allSent &= SendParam("p,9,"+ Math.round(GimbalCfg.getKdX() * 100) );
        allSent &= SendParam("p,10,"+ Math.round(GimbalCfg.getKpY() * 100) );
        allSent &= SendParam("p,11,"+ Math.round(GimbalCfg.getKiY() * 100) );
        allSent &= SendParam("p,12,"+ Math.round(GimbalCfg.getKdY() * 100) );
        allSent &= SendParam("p,13,"+ GimbalCfg.getServoXMin());
        allSent &= SendParam("p,14,"+ GimbalCfg.getServoXMax());
        allSent &= SendParam("p,15,"+ GimbalCfg.getServoYMin());
        allSent &= SendParam("p,16,"+ GimbalCfg.getServoYMax());
        allSent &= SendParam("p,17,"+ GimbalCfg.getConnectionSpeed());
        allSent &= SendParam("p,18,"+ GimbalCfg.getAltimeterResolution());
        allSent &= SendParam("p,19,"+ GimbalCfg.getEepromSize());
        allSent &= SendParam("p,20,"+ GimbalCfg.getUnits());
        allSent &= SendParam("p,21,"+ GimbalCfg.getEndRecordAltitude());
        allSent &= SendParam("p,22,"+ GimbalCfg.getBeepingFrequency());
        allSent &= SendParam("p,23,"+ GimbalCfg.getLiftOffDetect());
        allSent &= SendParam("p,24,"+ GimbalCfg.getGyroRange());
        allSent &= SendParam("p,25,"+ GimbalCfg.getAcceleroRange());
        allSent &= SendParam("p,26,"+ GimbalCfg.getRecordingTimeout());
        allSent &= SendParam("p,27,"+ GimbalCfg.getBatteryType());
        allSent &= SendParam("p,28,"+ GimbalCfg.getTelemetryType());
        if (myBT.getConnected()) {

            String myMessage = "";

            if (allSent) {
                myBT.setDataReady(false);
                myBT.flush();
                myBT.clearInput();
                //Write the config structure
                myBT.write("q;".toString());
                Log.d("conftab", "write config");

                //wait for the result to come back
                // ReadResult() waits for the answer itself (bounded by its timeout)
                myMessage = "";
                myMessage = myBT.ReadResult(3000);
                //msg(getResources().getString(R.string.msg3));
            } else {
                // A parameter was refused or lost: do not save a configuration that mixes old
                // and new values (PID gains, servo limits) in the board EEPROM. The previous
                // saved configuration is kept; the user is asked to try again.
                Log.w("conftab", "Configuration not saved: at least one parameter was not acknowledged");
            }

            myBT.setDataReady(false);
            myBT.flush();
            myBT.clearInput();
            //switch on the main loop before sending the config
            myBT.write("m1;".toString());
            Log.d("conftab", "switch on main loop");

            //wait for the result to come back
            // ReadResult() waits for the answer itself (bounded by its timeout)
            myMessage = "";
            myMessage = myBT.ReadResult(3000);
            //msg(getResources().getString(R.string.msg3));

            myBT.flush();
        }
        return allSent;
    }

    /**
     * Sends one configuration parameter ("p,index,value,checksum;") and waits for "OK".
     * Blocking: called from a background thread.
     *
     * @param altiCfgStr parameter without checksum, e.g. "p,7,29"
     * @return true if the board acknowledged the parameter
     */
    private boolean SendParam (String altiCfgStr) {
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
            // ReadResult() waits for the answer itself (bounded by its timeout)
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
            return myMessage.equals("OK");
        }
        return false;
    }
    public static Integer generateCheckSum(String value)  {

        byte[] data = value.getBytes();
        long checksum = 0L;

        for( byte b : data )  {
            checksum += b;
        }

        checksum = checksum % 256;

        return (int) checksum; // same value as before: fits in an int after the modulo

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