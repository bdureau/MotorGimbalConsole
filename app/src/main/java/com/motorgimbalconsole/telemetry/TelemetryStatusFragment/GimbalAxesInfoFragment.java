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
This is the first tab which contains
The gyros values
the accelerometer
the orientation on all axis
 */
public class GimbalAxesInfoFragment extends Fragment {
    private static final String TAG = "Tab1StatusFragment";
    private boolean ViewCreated = false;
    private TextView txtViewGyroXValue,txtViewGyroYValue, txtViewGyroZValue;
    private TextView txtViewAccelXValue, txtViewAccelYValue, txtViewAccelZValue;
    private TextView txtViewOrientXValue, txtViewOrientYValue, txtViewOrientZValue;

    public void setGyroXValue(String value) {
        if(ViewCreated)
            this.txtViewGyroXValue.setText(value);
    }
    public void setGyroYValue(String value) {
        if(ViewCreated)
            this.txtViewGyroYValue.setText(value);
    }
    public void setGyroZValue(String value) {
        if(ViewCreated)
            this.txtViewGyroZValue.setText(value);
    }
    public void setAccelXValue(String value) {
        if(ViewCreated)
            this.txtViewAccelXValue.setText(value);
    }
    public void setAccelYValue(String value) {
        if(ViewCreated)
            this.txtViewAccelYValue.setText(value);
    }
    public void setAccelZValue(String value) {
        if(ViewCreated)
            this.txtViewAccelZValue.setText(value);
    }
    public void setOrientXValue(String value) {
        if(ViewCreated)
            this.txtViewOrientXValue.setText(value);
    }
    public void setOrientYValue(String value) {
        if(ViewCreated)
            this.txtViewOrientYValue.setText(value);
    }
    public void setOrientZValue(String value) {
        if(ViewCreated)
            this.txtViewOrientZValue.setText(value);
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gimbal_status_part1,container,false);
        txtViewGyroXValue =(TextView)view.findViewById(R.id.textViewGyroXValue);
        txtViewGyroYValue =(TextView)view.findViewById(R.id.textViewGyroYValue);
        txtViewGyroZValue =(TextView)view.findViewById(R.id.textViewGyroZValue);
        txtViewAccelXValue =(TextView)view.findViewById(R.id.textViewAccelXValue);
        txtViewAccelYValue =(TextView)view.findViewById(R.id.textViewAccelYValue);
        txtViewAccelZValue =(TextView)view.findViewById(R.id.textViewAccelZValue);
        txtViewOrientXValue = (TextView)view.findViewById(R.id.textViewOrientXValue);
        txtViewOrientYValue = (TextView)view.findViewById(R.id.textViewOrientYValue);
        txtViewOrientZValue = (TextView)view.findViewById(R.id.textViewOrientZValue);

        ViewCreated = true;
        return view;
    }
}