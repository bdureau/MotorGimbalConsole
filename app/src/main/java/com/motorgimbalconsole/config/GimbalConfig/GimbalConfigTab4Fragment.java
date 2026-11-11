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
 * GimbalConfigTab4Fragment.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Fourth tab of the gimbal configuration: travel limits (minimum and maximum
 * angle, in degrees) of the X and Y gimbal servos. The limits are checked by
 * GimbalConfigValidator before upload.
 */
public class GimbalConfigTab4Fragment extends Fragment {
    private static final String TAG = "Tab4Fragment";
    private boolean ViewCreated = false;

    private EditText editTxtViewServoXMin, editTxtViewServoXMax, editTxtViewServoYMin, editTxtViewServoYMax;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    /**
     * No-argument constructor, required by Android: the system re-creates fragments with it
     * after a rotation or when the process is restored. The dependencies that used to be
     * passed as constructor parameters are taken from the application object instead.
     */
    public GimbalConfigTab4Fragment() {
        myBT = ConsoleApplication.getInstance();
        GimbalCfg = myBT.getGimbalConfigData();
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

    /**
     * Lists the input fields of this tab that are empty or not a number. Their getters return 0
     * in that case, a value that would otherwise be sent to the board without any warning.
     *
     * @return names of the invalid fields; empty if the tab view has not been created
     */
    public List<String> getInvalidFields() {
        List<String> invalid = new ArrayList<>();
        if (!ViewCreated) return invalid;
        if (!NumberParser.isInteger(editTxtViewServoXMin.getText().toString())) invalid.add("Servo X min");
        if (!NumberParser.isInteger(editTxtViewServoXMax.getText().toString())) invalid.add("Servo X max");
        if (!NumberParser.isInteger(editTxtViewServoYMin.getText().toString())) invalid.add("Servo Y min");
        if (!NumberParser.isInteger(editTxtViewServoYMax.getText().toString())) invalid.add("Servo Y max");
        return invalid;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gimbal_config_part4, container, false);

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

