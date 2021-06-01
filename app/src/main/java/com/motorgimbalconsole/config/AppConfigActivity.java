package com.motorgimbalconsole.config;

//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.MainActivityScreen;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.connection.SearchBluetooth;
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

public class AppConfigActivity extends AppCompatActivity {
    Button btnDismiss, btnSave, bdtDefault;
    /*private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
    private Spinner spConnectionType,spGraphicsLibType;*/
    private static AppConfigData appConfigData;

    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private TextToSpeech mTTS;

    private Tab1Fragment appConfigPage1 = null;
    private Tab2Fragment appConfigPage2 = null;

    ConsoleApplication myBT;

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
        //Language
      /*  spAppLanguage = (Spinner)findViewById(R.id.spinnerLanguage);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
        spAppLanguage.setAdapter(adapter);

        // graph color
        spGraphColor = (Spinner)findViewById(R.id.spinnerGraphColor);
       // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

        ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
        spGraphColor.setAdapter(adapterColor);
        // graph back color
        spGraphBackColor = (Spinner)findViewById(R.id.spinnerGraphBackColor);
        ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

        spGraphBackColor.setAdapter(adapterGraphColor);
        //units
        spAppUnit = (Spinner)findViewById(R.id.spinnerUnits);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
        spAppUnit.setAdapter(adapter2);

        //font size
        spFontSize = (Spinner)findViewById(R.id.spinnerFontSize);

        ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
        spFontSize.setAdapter(adapterFontSize);

        //Baud Rate
        spBaudRate = (Spinner)findViewById(R.id.spinnerBaudRate);

        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
        spBaudRate.setAdapter(adapterBaudRate);

        //connection type
        spConnectionType = (Spinner)findViewById(R.id.spinnerConnectionType);

        ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
        spConnectionType.setAdapter(adapterConnectionType);

        //Graphics lib type
        spGraphicsLibType = (Spinner)findViewById(R.id.spinnerGraphicLibType);
        ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
        spGraphicsLibType.setAdapter(adapterGraphicsLibType);*/

      //  ReadConfig();
    }

   /* void ReadConfig() {
        myBT.getAppConf().ReadConfig();
        appConfigPage1.spAppLanguage.setSelection(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        appConfigPage1.spAppUnit.setSelection(Integer.parseInt(myBT.getAppConf().getUnits()));
        appConfigPage1.spGraphColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        appConfigPage1.spGraphBackColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        appConfigPage1.spFontSize.setSelection((Integer.parseInt(myBT.getAppConf().getFontSize())-8));
        appConfigPage1.spBaudRate.setSelection(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        appConfigPage1.spConnectionType.setSelection(Integer.parseInt(myBT.getAppConf().getConnectionType()));
        appConfigPage1.spGraphicsLibType.setSelection(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));
    }*/

    void SaveConfig() {
        myBT.getAppConf().setApplicationLanguage(""+appConfigPage1.spAppLanguage.getSelectedItemId()+"");
        myBT.getAppConf().setUnits(""+appConfigPage1.spAppUnit.getSelectedItemId()+"");
        myBT.getAppConf().setGraphColor(""+appConfigPage1.spGraphColor.getSelectedItemId()+"");
        myBT.getAppConf().setGraphBackColor(""+appConfigPage1.spGraphBackColor.getSelectedItemId()+"");
        myBT.getAppConf().setFontSize(""+(appConfigPage1.spFontSize.getSelectedItemId()+8)+"");
        myBT.getAppConf().setBaudRate(""+appConfigPage1.spBaudRate.getSelectedItemId()+"");
        myBT.getAppConf().setConnectionType(""+appConfigPage1.spConnectionType.getSelectedItemId()+"");
        myBT.getAppConf().setGraphicsLibType(""+ appConfigPage1.spGraphicsLibType.getSelectedItemId()+"");
        myBT.getAppConf().setFullUSBSupport(appConfigPage1.getFullUSBSupport());


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
        appConfigPage1.spAppLanguage.setSelection(Integer.parseInt(myBT.getAppConf().getApplicationLanguage()));
        appConfigPage1.spAppUnit.setSelection(Integer.parseInt(myBT.getAppConf().getUnits()));
        appConfigPage1.spGraphColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphColor()));
        appConfigPage1.spGraphBackColor.setSelection(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
        appConfigPage1.spFontSize.setSelection(Integer.parseInt(myBT.getAppConf().getFontSize())-8);
        appConfigPage1.spBaudRate.setSelection(Integer.parseInt(myBT.getAppConf().getBaudRate()));
        appConfigPage1.spConnectionType.setSelection(Integer.parseInt(myBT.getAppConf().getConnectionType()));
        appConfigPage1.spGraphicsLibType.setSelection(Integer.parseInt(myBT.getAppConf().getGraphicsLibType()));

        if (myBT.getAppConf().getFullUSBSupport().equals("true")) {
            appConfigPage1.setFullUSBSupport(true);
        } else {
            appConfigPage1.setFullUSBSupport(false);
        }
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
            Intent i = new Intent(AppConfigActivity.this, HelpActivity.class);
            i.putExtra("help_file", "help_app_config");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(AppConfigActivity.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new AppConfigActivity.SectionsPageAdapter(getSupportFragmentManager());
        appConfigPage1 = new AppConfigActivity.Tab1Fragment(myBT);
        appConfigPage2 = new AppConfigActivity.Tab2Fragment(myBT);

        adapter.addFragment(appConfigPage1, "TAB1");
        adapter.addFragment(appConfigPage2, "TAB2");

        viewPager.setAdapter(adapter);
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
        private Spinner spAppLanguage, spGraphColor, spAppUnit, spGraphBackColor, spFontSize, spBaudRate;
        private Spinner spConnectionType,spGraphicsLibType;
        private CheckBox cbFullUSBSupport;
        private ConsoleApplication BT;
        public Tab1Fragment(ConsoleApplication lBT) {
            BT = lBT;
        }
        public String getFullUSBSupport() {
            if (cbFullUSBSupport.isChecked())
                return "true";
            else
                return "false";
        }

        public void setFullUSBSupport(boolean value) {
            cbFullUSBSupport.setChecked(value);
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            View view = inflater.inflate(R.layout.activity_app_config_part1, container, false);
            //Language
            spAppLanguage = (Spinner)view.findViewById(R.id.spinnerLanguage);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsLanguages());
            spAppLanguage.setAdapter(adapter);

            // graph color
            spGraphColor = (Spinner)view.findViewById(R.id.spinnerGraphColor);
            // String[] itemsColor = new String[]{"Black", "White", "Yellow", "Red", "Green", "Blue"};

            ArrayAdapter<String> adapterColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());
            spGraphColor.setAdapter(adapterColor);
            // graph back color
            spGraphBackColor = (Spinner)view.findViewById(R.id.spinnerGraphBackColor);
            ArrayAdapter<String> adapterGraphColor = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsColor());

            spGraphBackColor.setAdapter(adapterGraphColor);
            //units
            spAppUnit = (Spinner)view.findViewById(R.id.spinnerUnits);

            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsUnits());
            spAppUnit.setAdapter(adapter2);

            //font size
            spFontSize = (Spinner)view.findViewById(R.id.spinnerFontSize);

            ArrayAdapter<String> adapterFontSize = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsFontSize());
            spFontSize.setAdapter(adapterFontSize);

            //Baud Rate
            spBaudRate = (Spinner)view.findViewById(R.id.spinnerBaudRate);

            ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsBaudRate());
            spBaudRate.setAdapter(adapterBaudRate);

            //connection type
            spConnectionType = (Spinner)view.findViewById(R.id.spinnerConnectionType);

            ArrayAdapter<String> adapterConnectionType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsConnectionType());
            spConnectionType.setAdapter(adapterConnectionType);

            //Graphics lib type
            spGraphicsLibType = (Spinner)view.findViewById(R.id.spinnerGraphicLibType);
            ArrayAdapter<String> adapterGraphicsLibType = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, appConfigData.getItemsGraphicsLib());
            spGraphicsLibType.setAdapter(adapterGraphicsLibType);
            //Allow only telemetry via USB
            cbFullUSBSupport = (CheckBox) view.findViewById(R.id.checkBoxFullUSBSupport);

            spAppLanguage.setSelection(Integer.parseInt(BT.getAppConf().getApplicationLanguage()));
            spAppUnit.setSelection(Integer.parseInt(BT.getAppConf().getUnits()));
            spGraphColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphColor()));
            spGraphBackColor.setSelection(Integer.parseInt(BT.getAppConf().getGraphBackColor()));
            spFontSize.setSelection((Integer.parseInt(BT.getAppConf().getFontSize())-8));
            spBaudRate.setSelection(Integer.parseInt(BT.getAppConf().getBaudRate()));
            spConnectionType.setSelection(Integer.parseInt(BT.getAppConf().getConnectionType()));
            spGraphicsLibType.setSelection(Integer.parseInt(BT.getAppConf().getGraphicsLibType()));



            if (BT.getAppConf().getFullUSBSupport().equals("true")) {
                cbFullUSBSupport.setChecked(true);
            } else {
                cbFullUSBSupport.setChecked(false);
            }
            return view;
        }
    }
    public static class Tab2Fragment extends Fragment {
        private CheckBox  cbDrogueEvent, cbAltitudeEvent, cbLandingEvent;
        private CheckBox cbBurnoutEvent, cbWarningEvent, cbApogeeAltitude;
        private CheckBox cbLiftOffEvent;
        private ConsoleApplication BT;
        private Button btnTestVoice;
        private TextToSpeech mTTS;
        private Spinner spTelemetryVoice;
        private int nbrVoices = 0;

        public Tab2Fragment(ConsoleApplication lBT) {
            BT = lBT;
        }



        public String getDrogueEvent() {
            if (cbDrogueEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setDrogueEvent(boolean value) {
            cbDrogueEvent.setChecked(value);
        }

        public String getAltitudeEvent() {
            if (cbAltitudeEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setAltitudeEvent(boolean value) {
            cbAltitudeEvent.setChecked(value);
        }

        public String getLandingEvent() {
            if (cbLandingEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setLandingEvent(boolean value) {
            cbLandingEvent.setChecked(value);
        }

        public String getBurnoutEvent() {
            if (cbBurnoutEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setBurnoutEvent(boolean value) {
            cbBurnoutEvent.setChecked(value);
        }

        public String getWarningEvent() {
            if (cbWarningEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setWarningEvent(boolean value) {
            cbWarningEvent.setChecked(value);
        }

        public String getApogeeAltitude() {
            if (cbApogeeAltitude.isChecked())
                return "true";
            else
                return "false";
        }

        public void setApogeeAltitude(boolean value) {
            cbApogeeAltitude.setChecked(value);
        }



        //cbLiftOffEvent
        public String getLiftOffEvent() {
            if (cbLiftOffEvent.isChecked())
                return "true";
            else
                return "false";
        }

        public void setLiftOffEvent(boolean value) {
            cbLiftOffEvent.setChecked(value);
        }

        public void setVoices(String itemsVoices[]) {
            nbrVoices = itemsVoices.length;
            ArrayAdapter<String> adapterVoice = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, itemsVoices);
            spTelemetryVoice.setAdapter(adapterVoice);
            if (Integer.parseInt(BT.getAppConf().getTelemetryVoice()) < nbrVoices)
                spTelemetryVoice.setSelection(Integer.parseInt(BT.getAppConf().getTelemetryVoice()));
        }

        ;

        public void setTelemetryVoice(int value) {
            if (value < nbrVoices)
                this.spTelemetryVoice.setSelection(value);
        }

        public int getTelemetryVoice() {
            return (int) this.spTelemetryVoice.getSelectedItemId();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.activity_app_config_part2, container, false);
            //cbMainEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent1);
            cbDrogueEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent2);
            cbAltitudeEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent3);
            cbLandingEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent4);
            cbBurnoutEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent5);
            cbWarningEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent6);
            cbApogeeAltitude = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent7);
            //cbMainAltitude = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent8);
            cbLiftOffEvent = (CheckBox) view.findViewById(R.id.checkBoxAllowTelemetryEvent9);
            spTelemetryVoice = (Spinner) view.findViewById(R.id.spinnerTelemetryVoice);

           /* if (BT.getAppConf().getMain_event().equals("true")) {
                cbMainEvent.setChecked(true);
            } else {
                cbMainEvent.setChecked(false);
            }*/
            if (BT.getAppConf().getDrogue_event().equals("true")) {
                cbDrogueEvent.setChecked(true);
            } else {
                cbDrogueEvent.setChecked(false);
            }
            if (BT.getAppConf().getAltitude_event().equals("true")) {
                cbAltitudeEvent.setChecked(true);
            } else {
                cbAltitudeEvent.setChecked(false);
            }
            if (BT.getAppConf().getLanding_event().equals("true")) {
                cbLandingEvent.setChecked(true);
            } else {
                cbLandingEvent.setChecked(false);
            }
            if (BT.getAppConf().getBurnout_event().equals("true")) {
                cbBurnoutEvent.setChecked(true);
            } else {
                cbBurnoutEvent.setChecked(false);
            }
            if (BT.getAppConf().getWarning_event().equals("true")) {
                cbWarningEvent.setChecked(true);
            } else {
                cbWarningEvent.setChecked(false);
            }
            if (BT.getAppConf().getApogee_altitude().equals("true")) {
                cbApogeeAltitude.setChecked(true);
            } else {
                cbApogeeAltitude.setChecked(false);
            }
            /*if (BT.getAppConf().getMain_altitude().equals("true")) {
                cbMainAltitude.setChecked(true);
            } else {
                cbMainAltitude.setChecked(false);
            }*/
            //cbLiftOffEvent
            if (BT.getAppConf().getLiftOff_event().equals("true")) {
                cbLiftOffEvent.setChecked(true);
            } else {
                cbLiftOffEvent.setChecked(false);
            }


            btnTestVoice = (Button) view.findViewById(R.id.butTestVoice);
            btnTestVoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //init text to speech
                    mTTS = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                int result = 0;

                                if (Locale.getDefault().getLanguage() == "en")
                                    result = mTTS.setLanguage(Locale.ENGLISH);
                                else if (Locale.getDefault().getLanguage() == "fr")
                                    result = mTTS.setLanguage(Locale.FRENCH);
                                else
                                    result = mTTS.setLanguage(Locale.ENGLISH);

                                if (!BT.getAppConf().getTelemetryVoice().equals("")) {
                                    Log.d("Voice", BT.getAppConf().getTelemetryVoice());
                                    try {
                                        for (Voice tmpVoice : mTTS.getVoices()) {
                                            Log.d("Voice", tmpVoice.getName());
                                            if (tmpVoice.getName().equals(spTelemetryVoice.getSelectedItem().toString())) {
                                                mTTS.setVoice(tmpVoice);
                                                Log.d("Voice", "Found voice");
                                                break;
                                            }
                                        }
                                    } catch (Exception e) {
                                        msg(Locale.getDefault().getLanguage());
                                    }
                                }
                                mTTS.setPitch(1.0f);
                                mTTS.setSpeechRate(1.0f);
                                if (Locale.getDefault().getLanguage() == "en")
                                    mTTS.speak("Bearaltimeter altimeters are the best", TextToSpeech.QUEUE_FLUSH, null);

                                if (Locale.getDefault().getLanguage() == "fr")
                                    mTTS.speak("Les altimètres Bearaltimeter sont les meilleurs", TextToSpeech.QUEUE_FLUSH, null);

                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    Log.e("TTS", "Language not supported");
                                } else {
                                    //msg(Locale.getDefault().getLanguage());
                                }
                            } else {
                                Log.e("TTS", "Init failed");
                            }
                        }
                    });

                }
            });
            return view;
        }
        private void msg(String s) {
            Toast.makeText(getContext(), s, Toast.LENGTH_LONG).show();
        }
    }

}
