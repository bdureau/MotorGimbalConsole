package com.motorgimbalconsole.telemetry.TelemetryStatusFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.R;
/*
This is the second tab
it contains
The current altitude
the battery voltage
the current pressure
the current temperature of the sensor
the % of eeprom used
 */
public class GimbalInfoFragment extends Fragment {
    private static final String TAG = "Tab2StatusFragment";
    private TextView txtViewVoltage,txtNbrOfFlightValue;
    private boolean ViewCreated = false;
    private TextView txtViewBatteryVoltage,txtViewAltitudeValue;
    private TextView txtViewPressureValue,txtViewTempValue, txtViewEEpromUsageValue;

    public void setAltitudeValue(String value) {
        if(ViewCreated)
            this.txtViewAltitudeValue.setText(value);
    }
    public void setPressureValue(String value) {
        if(ViewCreated)
            this.txtViewPressureValue.setText(value);
    }
    public void setTempValue(String value) {
        if(ViewCreated)
            this.txtViewTempValue.setText(value);
    }
    public void setBatteryVoltage(String value) {
        if(ViewCreated)
            this.txtViewBatteryVoltage.setText(value);
    }
    public void setEEpromUsage(String value) {
        if(ViewCreated)
            this.txtViewEEpromUsageValue.setText(value);
    }
    public void setNbrOfFlights(String value) {
        if(ViewCreated)
            this.txtNbrOfFlightValue.setText(value);
    }
    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gimbal_status_part2,container,false);
        //
        txtViewAltitudeValue = (TextView)view.findViewById(R.id.txtAltitudeValue);
        txtViewPressureValue = (TextView)view.findViewById(R.id.txtPressureValue);
        txtViewTempValue = (TextView)view.findViewById(R.id.txtTempValue);
        txtViewVoltage=(TextView)view.findViewById(R.id.txtBatteryVoltage);
        txtViewBatteryVoltage=(TextView)view.findViewById(R.id.txtBatteryVoltageValue);
        txtViewEEpromUsageValue =(TextView)view.findViewById(R.id.txtEEpromUsageValue);
        txtNbrOfFlightValue=(TextView)view.findViewById(R.id.txtNbrOfFlightValue);
        ViewCreated = true;
        return view;
    }
}