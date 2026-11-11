package com.motorgimbalconsole.config.GimbalConfig;

import com.motorgimbalconsole.utils.NumberParser;
import java.util.ArrayList;
import java.util.List;
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

/**
 * GimbalConfigTab2Fragment.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Second tab of the gimbal configuration: PID gains (Kp, Ki, Kd) of the X and Y
 * gimbal axes. The gains are displayed as typed (e.g. 0.29) and sent to the board in
 * hundredths. A decimal comma is accepted; getInvalidFields() reports the empty or non
 * numeric gains.
 */
public class GimbalConfigTab2Fragment extends Fragment {
    private static final String TAG = "Tab2Fragment";
    private boolean ViewCreated = false;
    private EditText editTxtKpX, editTxtKiX, editTxtKdX;
    private EditText editTxtKpY, editTxtKiY, editTxtKdY;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    /**
     * No-argument constructor, required by Android: the system re-creates fragments with it
     * after a rotation or when the process is restored. The dependencies that used to be
     * passed as constructor parameters are taken from the application object instead.
     */
    public GimbalConfigTab2Fragment() {
        myBT = ConsoleApplication.getInstance();
        GimbalCfg = myBT.getGimbalConfigData();
    }

    public void setTxtKpXValue(double value) {
        this.editTxtKpX.setText(Double.toString(value));
    }

    public double getTxtKpXValue() {
        double ret;
        try {
            ret = NumberParser.decimalInputToDouble(this.editTxtKpX.getText().toString(), 0.0);
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
            ret = NumberParser.decimalInputToDouble(this.editTxtKiX.getText().toString(), 0.0);
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
            ret = NumberParser.decimalInputToDouble(this.editTxtKdX.getText().toString(), 0.0);
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
            ret = NumberParser.decimalInputToDouble(this.editTxtKpY.getText().toString(), 0.0);
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
            ret = NumberParser.decimalInputToDouble(this.editTxtKiY.getText().toString(), 0.0);
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
            ret = NumberParser.decimalInputToDouble(this.editTxtKdY.getText().toString(), 0.0);
        } catch (Exception e) {
            ret = 0.0;
        }
        return ret;
    }

    /**
     * Lists the input fields of this tab that are empty or not a number. Their getters return 0
     * in that case, a value that would otherwise be sent to the board without any warning.
     *
     * @return names of the invalid fields; empty if the tab view has not been created
     */
    public List<String> getInvalidFields() {
        List<String> invalid = new ArrayList<>();
        if (!ViewCreated) return invalid;
        if (!NumberParser.isDecimalInput(editTxtKpX.getText().toString())) invalid.add("Kp X");
        if (!NumberParser.isDecimalInput(editTxtKiX.getText().toString())) invalid.add("Ki X");
        if (!NumberParser.isDecimalInput(editTxtKdX.getText().toString())) invalid.add("Kd X");
        if (!NumberParser.isDecimalInput(editTxtKpY.getText().toString())) invalid.add("Kp Y");
        if (!NumberParser.isDecimalInput(editTxtKiY.getText().toString())) invalid.add("Ki Y");
        if (!NumberParser.isDecimalInput(editTxtKdY.getText().toString())) invalid.add("Kd Y");
        return invalid;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gimbal_config_part2, container, false);

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