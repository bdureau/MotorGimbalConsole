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

public class GimbalConfigTab2Fragment extends Fragment {
    private static final String TAG = "Tab2Fragment";
    private boolean ViewCreated = false;
    private EditText editTxtKpX, editTxtKiX, editTxtKdX;
    private EditText editTxtKpY, editTxtKiY, editTxtKdY;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    public GimbalConfigTab2Fragment(ConsoleApplication pBT,
                                    GimbalConfigData pGimbalCfg
    ) {
        myBT = pBT;
        GimbalCfg = pGimbalCfg;
    }

    public void setTxtKpXValue(double value) {
        this.editTxtKpX.setText(Double.toString(value));
    }

    public double getTxtKpXValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKpX.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    public void setTxtKiXValue(double value) {
        this.editTxtKiX.setText(Double.toString(value));
    }

    public double getTxtKiXValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKiX.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    public void setTxtKdXValue(double value) {
        this.editTxtKdX.setText(Double.toString(value));
    }

    public double getTxtKdXValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKdX.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;

    }

    public void setTxtKpYValue(double value) {
        this.editTxtKpY.setText(Double.toString(value));
    }

    public double getTxtKpYValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKpY.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    public void setTxtKiYValue(double value) {
        //this.editTxtKiY.setText(String.format("%.2f",value));
        this.editTxtKiY.setText(Double.toString(value));
    }

    public double getTxtKiYValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKiY.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    public void setTxtKdYValue(double value) {
        this.editTxtKdY.setText(Double.toString(value));
    }

    public double getTxtKdYValue() {
        double ret;
        try {
            ret = Double.parseDouble(this.editTxtKdY.getText().toString());
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tabconfigpart2_fragment, container, false);

        editTxtKpX = (EditText) view.findViewById(R.id.editTxtKpX);
        editTxtKiX = (EditText) view.findViewById(R.id.editTxtKiX);
        editTxtKdX = (EditText) view.findViewById(R.id.editTxtKdX);

        editTxtKpY = (EditText) view.findViewById(R.id.editTxtKpY);
        editTxtKiY = (EditText) view.findViewById(R.id.editTxtKiY);
        editTxtKdY = (EditText) view.findViewById(R.id.editTxtKdY);

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