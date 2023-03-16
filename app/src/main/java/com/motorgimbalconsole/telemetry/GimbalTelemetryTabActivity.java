package com.motorgimbalconsole.telemetry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.data.Entry;
import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.GlobalConfig;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.ShareHandler;
import com.motorgimbalconsole.flights.FlightData;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalAxesInfoFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalInfoFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalMpFlightFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalFcFlightFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalRocketViewFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GimbalTelemetryTabActivity extends AppCompatActivity {

    public String TAG = "TelemetryMp.class";
    private ViewPager mViewPager;
    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    private SectionsStatusPageAdapter adapter;
    private GimbalMpFlightFragment statusPage0 = null;
    private GimbalFcFlightFragment statusPage0bis = null;
    private GimbalAxesInfoFragment statusPage1 = null;
    private GimbalInfoFragment statusPage2 = null;
    private GimbalRocketViewFragment statusPage3 = null;

    private FlightData myflight = null; //used with afreeChart

    ArrayList<Entry> yValues;

    private ConsoleApplication myBT;
    Thread rocketTelemetry;

    //telemetry var
    private long LiftOffTime = 0;
    private int lastPlotTime = 0;
    private int lastSpeakTime = 1000;
    private double FEET_IN_METER = 1;

    private int altitudeTime = 0;

    private boolean telemetry = true;

    private boolean liftOffSaid = false;
    private boolean apogeeSaid = false;
    private boolean landedSaid = false;

    private boolean hasLiftOff = false;
    private boolean hasLanded = false;
    private boolean hasReachApogee = false;


    private Button dismissButton;

    private TextToSpeech mTTS;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //Value 1 contains the GyroX
                    statusPage1.setGyroXValue((String) msg.obj);
                    break;
                case 2:
                    //Value 2 contains the GyroY
                    statusPage1.setGyroYValue((String) msg.obj);
                    break;
                case 3:
                    //Value 3 contains the GyroZ
                    statusPage1.setGyroZValue((String) msg.obj);
                    break;
                case 4:
                    //Value 4 contains the AccelX
                    statusPage1.setAccelXValue((String) msg.obj);
                    break;
                case 5:
                    //Value 5 contains the AccelY
                    statusPage1.setAccelYValue((String) msg.obj);
                    break;
                case 6:
                    //Value 6 contains the AccelZ
                    statusPage1.setAccelZValue((String) msg.obj);
                    break;
                case 7:
                    //Value 7 contains the OrientX
                    statusPage1.setOrientXValue((String) msg.obj);
                    break;
                case 8:
                    //Value 8 contains the OrientY
                    statusPage1.setOrientYValue((String) msg.obj);
                    break;
                case 9:
                    // Value 9 contains the OrientZ
                    statusPage1.setOrientZValue((String) msg.obj);
                    break;
                case 10:
                    // Value 1 contain the current altitude
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        //Log.d("Altitude plot", "In altitude");
                        int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);
                        if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            //if (statusPage0bis.isLiftOffChecked() && !statusPage0bis.isLandedChecked()) {
                            if(hasLiftOff && !hasLanded) {
                                statusPage0bis.setCurrentAltitude(altitude + "");
                                myflight.AddToFlight(altitudeTime, altitude, "Telemetry");
                                //plot every seconde
                                if ((altitudeTime - lastPlotTime) > 1000) {
                                    lastPlotTime = altitudeTime;
                                    statusPage0bis.plotYvalues(myflight.GetFlightData("Telemetry"));
                                }
                            }
                        } else {
                            //if (statusPage0.isLiftOffChecked() && !statusPage0.isLandedChecked()) {
                            if(hasLiftOff && !hasLanded) {
                                statusPage0.setCurrentAltitude(String.valueOf(altitude));
                                yValues.add(new Entry(altitudeTime, altitude));
                                //plot every seconde
                                if ((altitudeTime - lastPlotTime) > 1000) {
                                    lastPlotTime = altitudeTime;
                                    Log.d("Altitude plot", String.valueOf(altitude));
                                    statusPage0.plotYvalues(yValues);
                                }
                            }
                        }
                        // Tell altitude every 5 secondes
                        if ((altitudeTime - lastSpeakTime) > 5000 && liftOffSaid && !landedSaid) {
                            if (myBT.getAppConf().getAltitude_event()) {
                                Log.d("Altitude plot", "say:" + String.valueOf(altitude));
                                mTTS.speak(getResources().getString(R.string.altitude) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                            }
                            lastSpeakTime = altitudeTime;
                        }
                        statusPage2.setAltitudeValue((String) msg.obj);
                    }
                    break;
                case 11:
                    // Value 11 contains the temperature
                    statusPage2.setTempValue((String) msg.obj);
                    break;
                case 12:
                    //Value 12 contains the pressure
                    statusPage2.setPressureValue((String) msg.obj);
                    break;

                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String) msg.obj;
                    if (voltage.matches("\\d+(?:\\.\\d+)?")) {
                        statusPage2.setBatteryVoltage(voltage + " Volts");
                    } else {
                        //txtViewVoltage.setText("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the graphic
                    statusPage3.setInputString((String) msg.obj + ",");
                    break;
                case 19:
                    //Value 19 contains the eeprom usage
                    statusPage2.setEEpromUsage((String) msg.obj + "%");
                    break;
                case 20:
                    //Value 20 contains the angle correction
                    statusPage3.setInputCorrect((String) msg.obj);
                    break;
                case 21:
                    //Value 21 contains the servoX
                    statusPage3.setServoX((String) msg.obj);
                    break;
                case 22:
                    //Value 22 contains the servoY
                    statusPage3.setServoY((String) msg.obj);
                    break;
                case 23:
                    // Value 23 lift off yes/no
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        //if (!statusPage0bis.isLiftOffChecked())
                        if(!hasLiftOff)
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                    statusPage0bis.setLiftOffChecked(true);
                                    hasLiftOff = true;
                                    if (LiftOffTime == 0)
                                        LiftOffTime = System.currentTimeMillis();
                                    statusPage0bis.setLiftOffTime("0 ms");
                                    if (!liftOffSaid) {
                                        if (myBT.getAppConf().getLiftOff_event()) {
                                            mTTS.speak(getResources().getString(R.string.lift_off), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        liftOffSaid = true;
                                    }
                                }
                    } else {
                        //if (!statusPage0.isLiftOffChecked())
                        if(!hasLiftOff)
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0 || LiftOffTime > 0) {
                                    statusPage0.setLiftOffChecked(true);
                                    hasLiftOff = true;
                                    if (LiftOffTime == 0)
                                        LiftOffTime = System.currentTimeMillis();
                                    statusPage0.setLiftOffTime("0 ms");
                                    if (!liftOffSaid) {
                                        if (myBT.getAppConf().getLiftOff_event()) {
                                            //lift_off
                                            mTTS.speak(getResources().getString(R.string.lift_off), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        liftOffSaid = true;
                                    }
                                }
                    }
                    break;
                case 24:
                    // Value 3 apogee fired yes/no
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        //if (statusPage0bis.isViewCreated())
                        Log.d(TAG, "Apogee afreechart");
                            //if (!statusPage0bis.isApogeeChecked())
                            if(!hasReachApogee)
                                if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                    if (Integer.parseInt((String) msg.obj) > 0) {
                                        statusPage0bis.setApogeeChecked(true);
                                        hasReachApogee = true;
                                        statusPage0bis.setMaxAltitudeTime((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                    }
                    } else {
                        //Log.d(TAG, "Apogee MPchart");
                        //if (!statusPage0.isApogeeChecked()) {
                        if(!hasReachApogee){
                            //Log.d(TAG, "!isApogeeChecked:" + (String) msg.obj);
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    Log.d(TAG, "setApogeeChecked");
                                    statusPage0.setApogeeChecked(true);
                                    hasReachApogee = true;
                                    statusPage0.setMaxAltitudeTime((int) (System.currentTimeMillis() - LiftOffTime) + " ms");
                                }
                            }
                        }
                    }
                    break;
                case 25:
                    //Value 4 apogee altitude
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                            //if (statusPage0bis.isApogeeChecked()) {
                            if(hasReachApogee) {
                                statusPage0bis.setMaxAltitude(altitude + "");
                                if (!apogeeSaid) {
                                    //first check if say it is enabled
                                    if (myBT.getAppConf().getApogee_altitude()) {
                                        mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    apogeeSaid = true;
                                }
                            }
                        }
                    } else {
                        if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                            int altitude = (int) (Integer.parseInt((String) msg.obj) * FEET_IN_METER);

                            //if (statusPage0.isApogeeChecked()) {
                            if(hasReachApogee) {
                                statusPage0.setMaxAltitude(String.valueOf(altitude));
                                //Log.d("Apogee", "apogee value :" + String.valueOf(altitude));
                                if (!apogeeSaid) {
                                    //first check if say it is enabled
                                    if (myBT.getAppConf().getApogee_altitude()) {
                                        //Log.d("Apogee", "say apogee value");
                                        mTTS.speak(getResources().getString(R.string.telemetry_apogee) + " " + String.valueOf(altitude) + " " + myBT.getAppConf().getUnitsValue(), TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                    apogeeSaid = true;
                                }
                            }
                        }
                    }
                    break;

                case 26:
                    //have we landed
                    if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                        //if (!statusPage0bis.isLandedChecked())
                        if(!hasLanded)
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    statusPage0bis.setLandedChecked(true);
                                    hasLanded = true;
                                    statusPage0bis.setLandedAltitude(statusPage0bis.getLandedAltitude());
                                    statusPage0bis.setLandedTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                    if (!landedSaid) {
                                        if (myBT.getAppConf().getLanding_event()) {
                                            mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        landedSaid = true;
                                    }
                                }
                    } else {
                        //if (!statusPage0.isLandedChecked())
                        if(!hasLanded)
                            if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?"))
                                if (Integer.parseInt((String) msg.obj) > 0) {
                                    statusPage0.setLandedChecked(true);
                                    hasLanded = true;
                                    statusPage0.setLandedAltitude(statusPage0.getLandedAltitude());
                                    statusPage0.setLandedTime((System.currentTimeMillis() - LiftOffTime) + " ms");
                                    if (!landedSaid) {
                                        if (myBT.getAppConf().getLanding_event()) {
                                            //  if (Locale.getDefault().getLanguage() == "en")
                                            mTTS.speak(getResources().getString(R.string.rocket_has_landed), TextToSpeech.QUEUE_FLUSH, null);
                                        }
                                        landedSaid = true;
                                    }
                                }
                    }
                    break;
                case 27:
                    // Value 27 contain the sample time
                    if (((String) msg.obj).matches("\\d+(?:\\.\\d+)?")) {
                        if (Integer.parseInt((String) msg.obj) > 0) {
                            altitudeTime = Integer.parseInt((String) msg.obj);
                        }
                    }
                    break;
                case 28:
                    //Value 28 contains the nbr of flight
                    statusPage2.setNbrOfFlights((String) msg.obj);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry_tab);
        yValues = new ArrayList<>();

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();

        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        dismissButton = (Button) findViewById(R.id.butDismiss);

        myBT.setHandler(handler);

        // Read the application config
        myBT.getAppConf().ReadConfig();

        //init text to speech
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    if (Locale.getDefault().getLanguage().equals("en"))
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage().equals("fr"))
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else if (Locale.getDefault().getLanguage().equals("nl"))
                        result = mTTS.setLanguage(new Locale("nl_NL"));
                    else if (Locale.getDefault().getLanguage().equals("it"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else if (Locale.getDefault().getLanguage().equals("ru"))
                        result = mTTS.setLanguage(getResources().getConfiguration().locale);
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);

                    //if (myBT.getAppConf().getTelemetryVoice()!=-1) {
                    // Log.d("Voice", myBT.getAppConf().getTelemetryVoice());
                    //String[] itemsVoices;
                    //String items = "";
                    int i = 0;
                    try {
                        for (Voice tmpVoice : mTTS.getVoices()) {

                            if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                Log.d("Voice", tmpVoice.getName());
                                if (myBT.getAppConf().getTelemetryVoice() == i) {
                                    mTTS.setVoice(tmpVoice);
                                    Log.d("Voice", "Found voice");
                                    break;
                                }
                                i++;
                            }
                        }
                    } catch (Exception e) {

                    }
                    //}

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        });
        mTTS.setPitch(1.0f);
        mTTS.setSpeechRate(1.0f);


        String myUnits = "";
        if (myBT.getAppConf().getUnits() == GlobalConfig.AltitudeUnit.METERS) {
            //Meters
            FEET_IN_METER = 1;
            myUnits = getResources().getString(R.string.Meters_fview);
        } else {
            //Feet
            FEET_IN_METER = 3.28084;
            myUnits = getResources().getString(R.string.Feet_fview);
        }

        startTelemetry();
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        finish();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        try {
            mTTS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (telemetry) {
            telemetry = false;
            myBT.write("h;".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }
        //turn off telemetry
        myBT.flush();
        myBT.clearInput();
        myBT.write("y0;".toString());
        //mTTS.shutdown();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (myBT.getConnected() && !telemetry) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            telemetry = true;
            //rocketTelemetry.start();
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!telemetry) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            rocketTelemetry = new Thread(r);
            rocketTelemetry.start();
        }
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop()");
        super.onStop();
        try {
            mTTS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (telemetry) {
            telemetry = false;
            myBT.write("h;".toString());

            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }


        myBT.flush();
        myBT.clearInput();
        myBT.write("h;".toString());
        try {
            while (myBT.getInputStream().available() <= 0) ;
        } catch (IOException e) {

        }
        String myMessage = "";
        long timeOut = 10000;
        long startTime = System.currentTimeMillis();

        myMessage = myBT.ReadResult(100000);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        if ((myBT.getAppConf().getGraphicsLibType() == GlobalConfig.GraphLib.AfreeChart) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
            statusPage0bis = new GimbalFcFlightFragment(myBT);
            adapter.addFragment(statusPage0bis, "TAB0");
        } else {
            statusPage0 = new GimbalMpFlightFragment(myBT);
            adapter.addFragment(statusPage0, "TAB0");
        }
        statusPage1 = new GimbalAxesInfoFragment();
        statusPage2 = new GimbalInfoFragment();
        statusPage3 = new GimbalRocketViewFragment();

        adapter.addFragment(statusPage1, "TAB1");
        adapter.addFragment(statusPage2, "TAB2");
        adapter.addFragment(statusPage3, "TAB3");

        linearDots = findViewById(R.id.idAltiStatusLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);

    }

    public void agregaIndicateDots(int pos, int nbr) {
        dotsSlide = new TextView[nbr];
        linearDots.removeAllViews();

        for (int i = 0; i < dotsSlide.length; i++) {
            dotsSlide[i] = new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if (dotsSlide.length > 0) {
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
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

    public class SectionsStatusPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsStatusPageAdapter(FragmentManager fm) {
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


    public void startTelemetry() {
        telemetry = true;

        lastPlotTime = 0;
        myBT.initFlightData();

        myflight = myBT.getFlightData(); //this is used by the afreeChart
        LiftOffTime = 0;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (!telemetry) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        rocketTelemetry = new Thread(r);
        rocketTelemetry.start();

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
            Intent i = new Intent(GimbalTelemetryTabActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_telemetry_alti");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(GimbalTelemetryTabActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
