package com.motorgimbalconsole.config;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
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

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.ShareHandler;
import com.motorgimbalconsole.config.AppConfig.AppConfigTab1Fragment;
import com.motorgimbalconsole.config.AppConfig.AppConfigTab2Fragment;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *   @description: In this activity you should be able to choose the application languages and looks and feel.
 *   Still a lot to do but it is a good start
 *   @author: boris.dureau@neuf.fr
 **/

public class AppTabConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;

    private static AppConfigData appConfigData;

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private TextToSpeech mTTS;

    private AppConfigTab1Fragment appConfigPage1 = null;
    private AppConfigTab2Fragment appConfigPage2 = null;

    ConsoleApplication myBT;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the Connection Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);
        // get the data for all the drop down
        appConfigData = new AppConfigData(this);
        setContentView(R.layout.activity_app_config);

        mViewPager = (ViewPager) findViewById(R.id.container_config);
        setupViewPager(mViewPager);

        myBT.getAppConf().ReadConfig();
        btnDismiss = (Button)findViewById(R.id.butDismiss);
        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();      //exit the application configuration activity
            }
        });

        btnSave = (Button)findViewById(R.id.butSave);
        btnSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //save the application configuration
                SaveConfig();
            }
        });

        bdtDefault= (Button)findViewById(R.id.butDefault);
        bdtDefault.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //restore the application default configuration
               RestoreToDefault();
            }
        });

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = 0;

                    if (Locale.getDefault().getLanguage() == "en")
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    else if (Locale.getDefault().getLanguage() == "fr")
                        result = mTTS.setLanguage(Locale.FRENCH);
                    else if (Locale.getDefault().getLanguage() == "nl")
                        result = mTTS.setLanguage(new Locale("nl_NL"));
                    else
                        result = mTTS.setLanguage(Locale.ENGLISH);
                    try {
                        String[] itemsVoices;
                        String items = "";
                        for (Voice tmpVoice : mTTS.getVoices()) {
                            if (tmpVoice.getName().startsWith(Locale.getDefault().getLanguage())) {
                                if (items.equals(""))
                                    items = tmpVoice.getName();
                                else
                                    items = items + "," + tmpVoice.getName();
                                Log.d("Voice", tmpVoice.getName());
                            }
                        }

                        itemsVoices = items.split(",");

                        appConfigPage2.setVoices(itemsVoices);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "Language not supported");
                        } else {

                        }
                    } catch (Exception e) {

                    }
                } else {
                    Log.e("TTS", "Init failed");
                }
            }
        }, "com.google.android.tts");

    }


    void SaveConfig() {

        myBT.getAppConf().setApplicationLanguage(""+appConfigPage1.getAppLanguage()+"");
        myBT.getAppConf().setUnits(""+appConfigPage1.getAppUnit()+"");
        myBT.getAppConf().setGraphColor(""+appConfigPage1.getGraphColor()+"");
        myBT.getAppConf().setGraphBackColor(""+appConfigPage1.getGraphBackColor()+"");
        myBT.getAppConf().setFontSize(""+(appConfigPage1.getFontSize()+8)+"");
        myBT.getAppConf().setBaudRate(""+appConfigPage1.getBaudRate()+"");
        myBT.getAppConf().setConnectionType(""+appConfigPage1.getConnectionType()+"");
        myBT.getAppConf().setGraphicsLibType(""+ appConfigPage1.getGraphicsLibType()+"");
        myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());
        myBT.getAppConf().setManualRecording(appConfigPage1.getAllowManualRecording());

        //page2
        myBT.getAppConf().setAltitude_event(appConfigPage2.getAltitudeEvent());
        myBT.getAppConf().setApogee_altitude(appConfigPage2.getApogeeAltitude());
        myBT.getAppConf().setBurnout_event(appConfigPage2.getBurnoutEvent());

        myBT.getAppConf().setDrogue_event(appConfigPage2.getDrogueEvent());
        myBT.getAppConf().setLanding_event(appConfigPage2.getLandingEvent());
        myBT.getAppConf().setWarning_event(appConfigPage2.getWarningEvent());

        myBT.getAppConf().setLiftOff_event(appConfigPage2.getLiftOffEvent());
        myBT.getAppConf().setTelemetryVoice("" + appConfigPage2.getTelemetryVoice() + "");

        myBT.getAppConf().SaveConfig();

        finish();
    }

    void RestoreToDefault() {
        myBT.getAppConf().ResetDefaultConfig();
        appConfigPage1.setAppLanguage(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        appConfigPage1.setAppUnit(Integer.parseInt(myBT.getAppConf().getUnits()));
        appConfigPage1.setGraphColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        appConfigPage1.setGraphBackColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        appConfigPage1.setFontSize(Integer.parseInt(myBT.getAppConf().getFontSize())-8);
        appConfigPage1.setBaudRate(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        appConfigPage1.setConnectionType(Integer.parseInt(myBT.getAppConf().getConnectionType()));
        appConfigPage1.setGraphicsLibType(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));

        if (myBT.getAppConf().getFullUSBSupport().equals("true")) {
            appConfigPage1.setFullUSBSupport(true);
        } else {
            appConfigPage1.setFullUSBSupport(false);
        }
        appConfigPage1.setAllowManualRecording(myBT.getAppConf().getManualRecording());

        //config page 2
        if (myBT.getAppConf().getAltitude_event().equals("true")) {
            appConfigPage2.setAltitudeEvent(true);
        } else {
            appConfigPage2.setAltitudeEvent(false);
        }
        if (myBT.getAppConf().getApogee_altitude().equals("true")) {
            appConfigPage2.setApogeeAltitude(true);
        } else {
            appConfigPage2.setApogeeAltitude(false);
        }
        if (myBT.getAppConf().getBurnout_event().equals("true")) {
            appConfigPage2.setBurnoutEvent(true);
        } else {
            appConfigPage2.setBurnoutEvent(false);
        }

        if (myBT.getAppConf().getDrogue_event().equals("true")) {
            appConfigPage2.setDrogueEvent(true);
        } else {
            appConfigPage2.setDrogueEvent(false);
        }
        if (myBT.getAppConf().getLanding_event().equals("true")) {
            appConfigPage2.setLandingEvent(true);
        } else {
            appConfigPage2.setLandingEvent(false);
        }

        if (myBT.getAppConf().getWarning_event().equals("true")) {
            appConfigPage2.setWarningEvent(true);
        } else {
            appConfigPage2.setWarningEvent(false);
        }
        if (myBT.getAppConf().getLiftOff_event().equals("true")) {
            appConfigPage2.setLiftOffEvent(true);
        } else {
            appConfigPage2.setLiftOffEvent(false);
        }
        appConfigPage2.setTelemetryVoice(Integer.parseInt(myBT.getAppConf().getTelemetryVoice()));

    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppTabConfigActivity.SectionsPageAdapter(getSupportFragmentManager());
        appConfigPage1 = new AppConfigTab1Fragment(myBT);
        appConfigPage2 = new AppConfigTab2Fragment(myBT);

        adapter.addFragment(appConfigPage1, "TAB1");
        adapter.addFragment(appConfigPage2, "TAB2");

        linearDots=findViewById(R.id.idAppConfigLinearDots);
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
            Intent i = new Intent(AppTabConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_app_config");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AppTabConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
