package com.motorgimbalconsole.telemetry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

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
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalAxesInfoFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalInfoFragment;
import com.motorgimbalconsole.telemetry.TelemetryStatusFragment.GimbalRocketViewFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: Gimbal real time telemetry
 * @author: boris.dureau@neuf.fr
 **/
public class GimbalTabStatusActivity extends AppCompatActivity {
    public String TAG = "GimbalTabStatusActivity";
    private ViewPager mViewPager;
    SectionsStatusPageAdapter adapter;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    private GimbalAxesInfoFragment statusPage1 =null;
    private GimbalInfoFragment statusPage2 =null;
    private GimbalRocketViewFragment statusPage3 =null;
    private Button btnDismiss, btnRecording;
    private ConsoleApplication myBT ;
    Thread altiStatus;
    boolean status = true;
    boolean recording = false;

    Handler handler = new Handler () {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //Value 1 contains the GyroX
                    statusPage1.setGyroXValue((String)msg.obj);
                    break;
                case 2:
                    //Value 2 contains the GyroY
                    statusPage1.setGyroYValue((String)msg.obj);
                    break;
                case 3:
                    //Value 3 contains the GyroZ
                    statusPage1.setGyroZValue((String)msg.obj);
                    break;
                case 4:
                    //Value 4 contains the AccelX
                    statusPage1.setAccelXValue((String)msg.obj);
                    break;
                case 5:
                    //Value 5 contains the AccelY
                    statusPage1.setAccelYValue((String)msg.obj);
                    break;
                case 6:
                    //Value 6 contains the AccelZ
                    statusPage1.setAccelZValue((String)msg.obj);
                    break;
                case 7:
                    //Value 7 contains the OrientX
                    statusPage1.setOrientXValue((String)msg.obj);
                    break;
                case 8:
                    //Value 8 contains the OrientY
                    statusPage1.setOrientYValue((String)msg.obj);
                    break;
                case 9:
                    // Value 9 contains the OrientZ
                    statusPage1.setOrientZValue((String)msg.obj);
                    break;
                case 10:
                    // Value 10 contains the altitude
                    statusPage2.setAltitudeValue((String)msg.obj);
                    break;
                case 11:
                    // Value 11 contains the temperature
                    statusPage2.setTempValue((String)msg.obj);
                    break;
                case 12:
                    //Value 12 contains the pressure
                    statusPage2.setPressureValue((String)msg.obj);
                    break;

                case 13:
                    //Value 13 contains the battery voltage
                    String voltage = (String)msg.obj;
                    if(voltage.matches("\\d+(?:\\.\\d+)?")) {
                        statusPage2.setBatteryVoltage(voltage + " Volts");
                    }
                    else {
                        //txtViewVoltage.setText("NA");
                    }
                    break;
                case 14:
                    //Value 14 contains the graphic
                    statusPage3.setInputString((String)msg.obj+",");
                    break;
                case 19:
                    //Value 19 contains the eeprom usage
                    statusPage2.setEEpromUsage((String)msg.obj+"%");
                    break;
                case 20:
                    //Value 20 contains the angle correction
                    statusPage3.setInputCorrect((String)msg.obj);
                    break;
                case 21:
                    //Value 21 contains the servoX
                    statusPage3.setServoX((String)msg.obj);
                    break;
                case 22:
                    //Value 22 contains the servoY
                    statusPage3.setServoY((String)msg.obj);
                    break;
                case 28:
                    //Value 28 contains the nbr of flight
                    statusPage2.setNbrOfFlights((String)msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_gimbal_tab_status);

        mViewPager =(ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        myBT.setHandler(handler);
        btnDismiss = (Button)findViewById(R.id.butDismiss);
        btnRecording = (Button)findViewById(R.id.butRecording);
        /*if ( myBT.getAppConf().getManualRecording())
            btnRecording.setVisibility(View.VISIBLE);
        else
            btnRecording.setVisibility(View.INVISIBLE);*/
        btnRecording.setVisibility(View.INVISIBLE);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
               /* if (status) {
                    status = false;
                    myBT.write("h;".toString());
                    myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                }
                if(myBT.getConnected()) {
                    //turn off telemetry
                    myBT.flush();
                    myBT.clearInput();
                    myBT.write("y0;".toString());
                }*/
                finish();      //exit the  activity
            }
        });

        btnRecording.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (recording) {
                    recording = false;
                    myBT.write("w0;".toString());
                    //myBT.setExit(true);
                    myBT.clearInput();
                    myBT.flush();
                    //btnRecording.setText("Start Rec");
                    msg("Stopped recording");
                }
                else
                {
                    recording = true;
                    myBT.write("w1;".toString());
                    myBT.clearInput();
                    myBT.flush();
                    //btnRecording.setText("Stop");
                    msg("Started recording");
                }

            }
        });

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(!status) break;
                    myBT.ReadResult(10000);
                }
            }
        };

        altiStatus = new Thread(r);
        altiStatus.start();
    }
    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        if (status) {
            status = false;
            myBT.write("h;".toString());
            myBT.setExit(true);
            myBT.clearInput();
            myBT.flush();
        }
        //if(myBT.getConnected()) {
            //turn off telemetry
            myBT.flush();
            myBT.clearInput();
            myBT.write("y0;".toString());
        //}
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if(myBT.getConnected() && !status) {
            myBT.flush();
            myBT.clearInput();

            myBT.write("y1;".toString());
            status = true;
            ///
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        if (!status) break;
                        myBT.ReadResult(10000);
                    }
                }
            };
            altiStatus = new Thread(r);
            //
            altiStatus.start();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");

        /*if (status) {
            status = false;
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
        //long startTime = System.currentTimeMillis();

        myMessage =myBT.ReadResult(timeOut);*/
    }
    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsStatusPageAdapter(getSupportFragmentManager());
        statusPage1 = new GimbalAxesInfoFragment();
        statusPage2 = new GimbalInfoFragment();
        statusPage3 = new GimbalRocketViewFragment();

        adapter.addFragment(statusPage1, "TAB1");
        adapter.addFragment(statusPage2, "TAB2");
        adapter.addFragment(statusPage3, "TAB3");

        linearDots=findViewById(R.id.idStatusLinearDots);
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


    public class SectionsStatusPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList= new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        public SectionsStatusPageAdapter (FragmentManager fm){
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
            Intent i = new Intent(GimbalTabStatusActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_gimbal_config");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(GimbalTabStatusActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
