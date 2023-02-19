package com.motorgimbalconsole.config.GimbalConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.config.GimbalConfigData;

public class GimbalConfigTab4Fragment extends Fragment {
    private static final String TAG = "Tab4Fragment";
    private boolean ViewCreated = false;

    private EditText editTxtViewServoXMin, editTxtViewServoXMax, editTxtViewServoYMin, editTxtViewServoYMax;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    public GimbalConfigTab4Fragment(ConsoleApplication pBT,
                                    GimbalConfigData pGimbalCfg
    ) {
        myBT = pBT;
        GimbalCfg = pGimbalCfg;
    }
    public void setServoXMin(int value) {
        this.editTxtViewServoXMin.setText(String.valueOf(value));
    }

    public int getServoXMin() {
        int ret;
        try {
            ret = Integer.parseInt(this.editTxtViewServoXMin.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setServoXMax(int value) {
        this.editTxtViewServoXMax.setText(String.valueOf(value));
    }

    public int getServoXMax() {
        int ret;
        try {
            ret = Integer.parseInt(this.editTxtViewServoXMax.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setServoYMin(int value) {
        this.editTxtViewServoYMin.setText(String.valueOf(value));
    }

    public int getServoYMin() {
        int ret;
        try {
            ret = Integer.parseInt(this.editTxtViewServoYMin.getText().toString());
        } catch (Exception e) {
            ret = 0;
        }
        return ret;
    }

    public void setServoYMax(int value) {
        this.editTxtViewServoYMax.setText(String.valueOf(value));
    }

    public int getServoYMax() {
        int ret;
        try {
            ret = Integer.parseInt(this.editTxtViewServoYMax.getText().toString());
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
        View view = inflater.inflate(R.layout.tabconfigpart4_fragment, container, false);

        editTxtViewServoXMin = (EditText) view.findViewById(R.id.editTxtServoXMin);
        editTxtViewServoXMax = (EditText) view.findViewById(R.id.editTxtServoXMax);
        editTxtViewServoYMin = (EditText) view.findViewById(R.id.editTxtServoYMin);
        editTxtViewServoYMax = (EditText) view.findViewById(R.id.editTxtServoYMax);

        if (GimbalCfg != null) {
            setServoXMin(GimbalCfg.getServoXMin());
            setServoXMax(GimbalCfg.getServoXMax());
            setServoYMax(GimbalCfg.getServoYMax());
            setServoYMin(GimbalCfg.getServoYMin());
        }
        ViewCreated = true;
        return view;
    }
}

