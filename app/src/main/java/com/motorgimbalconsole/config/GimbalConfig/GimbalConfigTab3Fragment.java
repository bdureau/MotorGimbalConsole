package com.motorgimbalconsole.config.GimbalConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.config.GimbalConfigData;

public class GimbalConfigTab3Fragment extends Fragment {
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
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    //txtAltiNameValue
    //spinnerUnit
    //editTxtBipFreq
    //spinnerBaudRate
    //spinnerAltimeterResolution
    //spinnerEEpromSize
    //txtViewEndRecordAltitude
    public GimbalConfigTab3Fragment(ConsoleApplication pBT,
                                    GimbalConfigData pGimbalCfg
    ) {
        myBT = pBT;
        GimbalCfg = pGimbalCfg;
    }
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
        View view = inflater.inflate(R.layout.fragment_gimbal_config_part3, container, false);

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
