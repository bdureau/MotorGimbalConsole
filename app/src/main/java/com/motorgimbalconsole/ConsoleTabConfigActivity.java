package com.motorgimbalconsole;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConsoleTabConfigActivity extends AppCompatActivity {
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    Tab1Fragment configPage1 =null;
    Tab2Fragment configPage2 =null;
    Tab3Fragment configPage3 =null;

    private Button btnDismiss, btnUpload;
    ConsoleApplication myBT ;
    private static GimbalConfigData GimbalCfg = null;

    private ProgressDialog progress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();
        readConfig();
        setContentView(R.layout.activity_console_tab_config);

        mViewPager =(ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        btnDismiss = (Button)findViewById(R.id.butDismiss);

        btnDismiss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                //myBT.flush();
                //myBT.clearInput();
                //myBT.write("y0;\n".toString());
                finish();      //exit the  activity
            }
        });
        btnUpload = (Button)findViewById(R.id.butUpload);
        btnUpload.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                //send back the config to the altimeter and exit if successful
                sendConfig();
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        configPage1 = new Tab1Fragment();
        configPage2 = new Tab2Fragment();
        configPage3 = new Tab3Fragment();

        adapter.addFragment(configPage1, "TAB1");
        adapter.addFragment(configPage2, "TAB2");
        adapter.addFragment(configPage3, "TAB3");


        viewPager.setAdapter(adapter);
    }

    private void readConfig()
    {
        // ask for config
        if(myBT.getConnected()) {
            //make sure that telemetry is off
            //myBT.write("y0;\n".toString());
            myBT.flush();
            myBT.clearInput();
            //msg("Retreiving altimeter config...");
            myBT.setDataReady(false);

            myBT.flush();
            myBT.clearInput();

            myBT.write("b;\n".toString());

            myBT.flush();


            //get the results
            //wait for the result to come back
            try {
                while (myBT.getInputStream().available() <= 0) ;
            } catch (IOException e) {

            }
        }
        //reading the config
        if(myBT.getConnected()) {
            String myMessage = "";
            long timeOut = 10000;
            long startTime = System.currentTimeMillis();

            myMessage =myBT.ReadResult(3000);

            //msg(myMessage);
            if (myMessage.equals( "start alticonfig end") )
            {
                try {
                   // getGimbalConfigData
                    GimbalCfg= myBT.getGimbalConfigData();
                    msg("we have a config");
                }
                catch (Exception e) {
                     msg("pb ready data");
                }
            }
            else
            {
                 msg("data not ready");
                //msg(myMessage);
            }
        }

    }
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private boolean sendConfig()
    {
        //final boolean exit_no_save = false;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        long prevBaudRate = GimbalCfg.getConnectionSpeed();

        // check if the baud rate has changed

        if(configPage1.isViewCreated()) {
            GimbalCfg.setAxOffset(configPage1.getAxOffsetValue());
            GimbalCfg.setAyOffset(configPage1.getAyOffsetValue());
            GimbalCfg.setAzOffset(configPage1.getAzOffsetValue());
            GimbalCfg.setGxOffset(configPage1.getGxOffsetValue());
            GimbalCfg.setGyOffset(configPage1.getGyOffsetValue());
            GimbalCfg.setGzOffset(configPage1.getGzOffsetValue());

        }
        if(configPage2.isViewCreated()) {

            GimbalCfg.setKpX(configPage2.getTxtKpXValue());
            GimbalCfg.setKiX(configPage2.getTxtKiXValue());
            GimbalCfg.setKdX(configPage2.getTxtKdXValue());
            GimbalCfg.setKpY(configPage2.getTxtKpYValue());
            GimbalCfg.setKiY(configPage2.getTxtKiYValue());
            GimbalCfg.setKdY(configPage2.getTxtKdYValue());


        }

        if(configPage3.isViewCreated()) {
            GimbalCfg.setBeepingFrequency(configPage3.getFreq());
            GimbalCfg.setUnits(configPage3.getDropdownUnits());
            GimbalCfg.setAltimeterResolution(configPage3.getAltimeterResolution());
            GimbalCfg.setEepromSize(configPage3.getEEpromSize());
            GimbalCfg.setEndRecordAltitude(configPage3.getEndRecordAltitude());

            GimbalCfg.setServoXMin(configPage3.getServoXMin());
            GimbalCfg.setServoXMax(configPage3.getServoXMax());
            GimbalCfg.setServoYMin(configPage3.getServoYMin());
            GimbalCfg.setServoYMax(configPage3.getServoYMax());
        }






        if(configPage3.isViewCreated()) {
            //GimbalCfg.setConnectionSpeed(configPage3.getBaudRate());
            if(GimbalCfg.getConnectionSpeed() != configPage3.getBaudRate())
            {
                //final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //You are about to change the baud rate, are you sure you want to do it?
                builder.setMessage(getResources().getString(R.string.msg9))
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                GimbalCfg.setConnectionSpeed(configPage3.getBaudRate());
                                sendAltiCfg ();
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
            }
            else
            {
                sendAltiCfg ();
                finish();
            }
        }
        else
        {
            sendAltiCfg ();
            finish();
        }

        return true;
    }
    private void sendAltiCfg () {
        String gimbalCfgStr="";

        gimbalCfgStr = "s," +
                GimbalCfg.getAxOffset()+","+
                GimbalCfg.getAyOffset()+","+
                GimbalCfg.getAzOffset()+","+
                GimbalCfg.getGxOffset()+","+
                GimbalCfg.getGyOffset()+","+
                GimbalCfg.getGzOffset()+","+
                GimbalCfg.getKpX()+","+
                GimbalCfg.getKiX()+","+
                GimbalCfg.getKdX()+","+
                GimbalCfg.getKpY()+","+
                GimbalCfg.getKiY()+","+
                GimbalCfg.getKdY()+","+
                GimbalCfg.getServoXMin() +","+
                GimbalCfg.getServoXMax() +","+
                GimbalCfg.getServoYMin() +","+
                GimbalCfg.getServoYMax() +","+
                GimbalCfg.getConnectionSpeed()+ ","+
                GimbalCfg.getAltimeterResolution()+ ","+
                GimbalCfg.getEepromSize()+"," +
                GimbalCfg.getUnits() +","+
                GimbalCfg.getEndRecordAltitude()+ ","+
                GimbalCfg.getBeepingFrequency();


                gimbalCfgStr = gimbalCfgStr +  ";\n";
         msg(gimbalCfgStr.toString());

        if(myBT.getConnected())
            //send back the config
            myBT.write(gimbalCfgStr.toString());

        //msg(getResources().getString(R.string.msg3));//+altiCfgStr.toString());

        myBT.flush();

        //return true;
    }
    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList= new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
        public SectionsPageAdapter (FragmentManager fm){
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
            View view = inflater.inflate(R.layout.tabconfigpart1_fragment,container,false);
            txtViewAxOffset= (EditText)view.findViewById(R.id.editTxtAxOffset);
            txtViewAyOffset= (EditText)view.findViewById(R.id.editTxtAyOffset);
            txtViewAzOffset= (EditText)view.findViewById(R.id.editTxtAzOffset);

            txtViewGxOffset= (EditText)view.findViewById(R.id.editTxtGxOffset);
            txtViewGyOffset= (EditText)view.findViewById(R.id.editTxtGyOffset);
            txtViewGzOffset= (EditText)view.findViewById(R.id.editTxtGzOffset);
            if (GimbalCfg != null) {
                setAxOffsetValue(GimbalCfg.getAxOffset());
                setAyOffsetValue(GimbalCfg.getAyOffset());
                setAzOffsetValue(GimbalCfg.getAzOffset());
                setGxOffsetValue(GimbalCfg.getGxOffset());
                setGyOffsetValue(GimbalCfg.getGyOffset());
                setGzOffsetValue(GimbalCfg.getGzOffset());
            }
            ViewCreated = true;
            return view;
        }
    }
    public static class Tab2Fragment extends Fragment {
        private static final String TAG = "Tab2Fragment";
        private boolean ViewCreated = false;
        private EditText editTxtKpX, editTxtKiX, editTxtKdX;
        private EditText editTxtKpY, editTxtKiY, editTxtKdY;

        public void setTxtKpXValue(double value) {
            this.editTxtKpX.setText(String.format("%.2f",value));
        }
        public double getTxtKpXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKpX.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setTxtKiXValue(double value) {
            this.editTxtKiX.setText(String.format("%.2f",value));
        }
        public double getTxtKiXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKiX.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setTxtKdXValue(double value) {
            this.editTxtKdX.setText(String.format("%.2f",value));
        }
        public double getTxtKdXValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKdX.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;

        }

        public void setTxtKpYValue(double value) {
            this.editTxtKpY.setText(String.format("%.2f",value));
        }
        public double getTxtKpYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKpY.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setTxtKiYValue(double value) {
            this.editTxtKiY.setText(String.format("%.2f",value));
        }
        public double getTxtKiYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKiY.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setTxtKdYValue(double value) {
            this.editTxtKdY.setText(String.format("%.2f",value));
        }
        public double getTxtKdYValue() {
            double ret;
            try {
                ret = Double.parseDouble(this.editTxtKdY.getText().toString());
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
            View view = inflater.inflate(R.layout.tabconfigpart2_fragment,container,false);

            editTxtKpX = (EditText)view.findViewById(R.id.editTxtKpX);
            editTxtKiX = (EditText)view.findViewById(R.id.editTxtKiX);
            editTxtKdX = (EditText)view.findViewById(R.id.editTxtKdX);

            editTxtKpY = (EditText)view.findViewById(R.id.editTxtKpY);
            editTxtKiY = (EditText)view.findViewById(R.id.editTxtKiY);
            editTxtKdY = (EditText)view.findViewById(R.id.editTxtKdY);

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

        private Spinner dropdownBaudRate;
        private Spinner dropdownAltimeterResolution, dropdownEEpromSize;

        private EditText EndRecordAltitude;
        private Spinner dropdownUnits;
        private TextView altiName;
        private EditText Freq;

        private EditText editTxtViewServoXMin, editTxtViewServoXMax, editTxtViewServoYMin, editTxtViewServoYMax;
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
            return (int)this.dropdownUnits.getSelectedItemId();
        }

        public void setDropdownUnits(int Units) {
            this.dropdownUnits.setSelection(Units);
        }


        public int getAltimeterResolution() {
            return (int)this.dropdownAltimeterResolution.getSelectedItemId();
        }
        public void setAltimeterResolution(int AltimeterResolution ) {
            this.dropdownAltimeterResolution.setSelection(AltimeterResolution);
        }

        public int getEEpromSize() {
            int ret;
            try {
                ret = Integer.parseInt(itemsEEpromSize[(int)dropdownEEpromSize.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setEEpromSize(int EEpromSize ) {
            this.dropdownEEpromSize.setSelection(GimbalCfg.arrayIndex(itemsEEpromSize, String.valueOf(EEpromSize)));
        }
        public long getBaudRate() {
            long ret;
            try {
                ret = Long.parseLong(itemsBaudRate[(int)this.dropdownBaudRate.getSelectedItemId()]);
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setBaudRate(long BaudRate ) {
            this.dropdownBaudRate.setSelection(GimbalCfg.arrayIndex(itemsBaudRate,String.valueOf(BaudRate)));
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
        public void setEndRecordAltitude(int EndRecordAltitude ) {
            this.EndRecordAltitude.setText(String.valueOf(EndRecordAltitude));
        }

        public void setServoXMin (int value) {
            this.editTxtViewServoXMin.setText(String.valueOf(value));
        }

        public int getServoXMin () {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoXMin.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServoXMax (int value) {
            this.editTxtViewServoXMax.setText(String.valueOf(value));
        }

        public int getServoXMax () {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoXMax.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        public void setServoYMin (int value) {
            this.editTxtViewServoYMin.setText(String.valueOf(value));
        }

        public int getServoYMin () {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoYMin.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }

        public void setServoYMax (int value) {
            this.editTxtViewServoYMax.setText(String.valueOf(value));
        }

        public int getServoYMax () {
            int ret;
            try {
                ret = Integer.parseInt(this.editTxtViewServoYMax.getText().toString());
            } catch (Exception e) {
                ret = 0;
            }
            return ret;
        }
        //, editTxtViewServoXMax, editTxtViewServoYMin, editTxtViewServoYMax;
        public boolean isViewCreated() {
            return ViewCreated;
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tabconfigpart3_fragment,container,false);

            //units
            dropdownUnits = (Spinner)view.findViewById(R.id.spinnerUnit);
            String[] items2 = new String[]{"Meters", "Feet"};
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, items2);
            dropdownUnits.setAdapter(adapter2);

            //Altimeter name
            altiName = (TextView)view.findViewById(R.id.txtAltiNameValue);
            //here you can set the beep frequency
            Freq=(EditText)view.findViewById(R.id.editTxtBipFreq);

            //baud rate
            dropdownBaudRate = (Spinner)view.findViewById(R.id.spinnerBaudRate);
            itemsBaudRate = new String[]{ "300",
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
            dropdownAltimeterResolution = (Spinner)view.findViewById(R.id.spinnerAltimeterResolution);
            itemsAltimeterResolution = new String[]{"ULTRALOWPOWER", "STANDARD","HIGHRES","ULTRAHIGHRES"};
            ArrayAdapter<String> adapterAltimeterResolution= new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsAltimeterResolution);
            dropdownAltimeterResolution.setAdapter(adapterAltimeterResolution);

            //Altimeter external eeprom size
            dropdownEEpromSize = (Spinner)view.findViewById(R.id.spinnerEEpromSize);
            itemsEEpromSize = new String[]{"32", "64","128","256","512", "1024"};
            ArrayAdapter<String> adapterEEpromSize= new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, itemsEEpromSize);
            dropdownEEpromSize.setAdapter(adapterEEpromSize);
            // nbr of meters to stop recording altitude
            EndRecordAltitude = (EditText)view.findViewById(R.id.editTxtEndRecordAltitude);

            editTxtViewServoXMin = (EditText)view.findViewById(R.id.editTxtServoXMin);
            editTxtViewServoXMax = (EditText)view.findViewById(R.id.editTxtServoXMax);
            editTxtViewServoYMin = (EditText)view.findViewById(R.id.editTxtServoYMin);
            editTxtViewServoYMax = (EditText)view.findViewById(R.id.editTxtServoYMax);

            if (GimbalCfg != null) {
                setBaudRate(GimbalCfg.getConnectionSpeed());
                //dropdownBaudRate.setSelection(GimbalCfg.arrayIndex(itemsBaudRate,String.valueOf(GimbalCfg.getConnectionSpeed())));
                dropdownAltimeterResolution.setSelection(GimbalCfg.getAltimeterResolution());
                dropdownEEpromSize.setSelection(GimbalCfg.arrayIndex(itemsEEpromSize, String.valueOf(GimbalCfg.getEepromSize())));
                EndRecordAltitude.setText(String.valueOf(GimbalCfg.getEndRecordAltitude()));

                altiName.setText(GimbalCfg.getAltimeterName()+ " ver: " +
                        GimbalCfg.getAltiMajorVersion()+"."+GimbalCfg.getAltiMinorVersion());


                dropdownUnits.setSelection(GimbalCfg.getUnits());
                Freq.setText(String.valueOf(GimbalCfg.getBeepingFrequency()));
                setServoXMin(GimbalCfg.getServoXMin());
                setServoXMax(GimbalCfg.getServoXMax());
                setServoYMax(GimbalCfg.getServoYMax());
                setServoYMin(GimbalCfg.getServoYMin());
            }
            ViewCreated = true;
            return view;
        }
    }
}