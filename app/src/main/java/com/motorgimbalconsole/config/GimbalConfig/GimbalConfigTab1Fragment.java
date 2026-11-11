package com.motorgimbalconsole.config.GimbalConfig;

import com.motorgimbalconsole.utils.NumberParser;
import java.util.ArrayList;
import java.util.List;
import static com.motorgimbalconsole.config.GimbalTabConfigActivity.readConfig;

import android.app.AlertDialog;
import android.content.DialogInterface;
import com.motorgimbalconsole.utils.BackgroundTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.config.GimbalConfigData;

/**
 * GimbalConfigTab1Fragment.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: First tab of the gimbal configuration (MPU6050 board only): accelerometer and
 * gyroscope offsets, and the calibration command ("c;") that measures them on the board and
 * reads them back. getInvalidFields() reports the empty or non numeric offsets.
 */
public class GimbalConfigTab1Fragment extends Fragment {
    private static final String TAG = "Tab1Fragment";
    private boolean ViewCreated = false;
    private EditText txtViewAxOffset, txtViewAyOffset, txtViewAzOffset;
    private EditText txtViewGxOffset, txtViewGyOffset, txtViewGzOffset;
    private Button btnCalibrate;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    /**
     * No-argument constructor, required by Android: the system re-creates fragments with it
     * after a rotation or when the process is restored. The dependencies that used to be
     * passed as constructor parameters are taken from the application object instead.
     */
    public GimbalConfigTab1Fragment() {
        myBT = ConsoleApplication.getInstance();
        GimbalCfg = myBT.getGimbalConfigData();
    }
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

    /**
     * Lists the input fields of this tab that are empty or not a number. Their getters return 0
     * in that case, a value that would otherwise be sent to the board without any warning.
     *
     * @return names of the invalid fields; empty if the tab view has not been created
     */
    public List<String> getInvalidFields() {
        List<String> invalid = new ArrayList<>();
        if (!ViewCreated) return invalid;
        if (!NumberParser.isInteger(txtViewAxOffset.getText().toString())) invalid.add("ax offset");
        if (!NumberParser.isInteger(txtViewAyOffset.getText().toString())) invalid.add("ay offset");
        if (!NumberParser.isInteger(txtViewAzOffset.getText().toString())) invalid.add("az offset");
        if (!NumberParser.isInteger(txtViewGxOffset.getText().toString())) invalid.add("gx offset");
        if (!NumberParser.isInteger(txtViewGyOffset.getText().toString())) invalid.add("gy offset");
        if (!NumberParser.isInteger(txtViewGzOffset.getText().toString())) invalid.add("gz offset");
        return invalid;
    }

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gimbal_config_part1, container, false);
        txtViewAxOffset = (EditText) view.findViewById(R.id.editTxtAxOffset);
        txtViewAyOffset = (EditText) view.findViewById(R.id.editTxtAyOffset);
        txtViewAzOffset = (EditText) view.findViewById(R.id.editTxtAzOffset);
        txtViewGxOffset = (EditText) view.findViewById(R.id.editTxtGxOffset);
        txtViewGyOffset = (EditText) view.findViewById(R.id.editTxtGyOffset);
        txtViewGzOffset = (EditText) view.findViewById(R.id.editTxtGzOffset);
        if (GimbalCfg != null) {
            setAxOffsetValue(GimbalCfg.getAxOffset());
            setAyOffsetValue(GimbalCfg.getAyOffset());
            setAzOffsetValue(GimbalCfg.getAzOffset());
            setGxOffsetValue(GimbalCfg.getGxOffset());
            setGyOffsetValue(GimbalCfg.getGyOffset());
            setGzOffsetValue(GimbalCfg.getGzOffset());
        }
        btnCalibrate = (Button) view.findViewById(R.id.butCalibrate);

        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Calibration().execute();
            }
        });
        ViewCreated = true;
        return view;
    }
    // calibration
    private class Calibration extends BackgroundTask  // replaces the deprecated AsyncTask (same callbacks)
    {
        private AlertDialog.Builder builder = null;
        private AlertDialog alert;
        private Boolean canceled = false;

        @Override
        protected void onPreExecute() {
            //"Calibration in progress..."
            //"Please wait!!!"
            //this.getActivity()
            builder = new AlertDialog.Builder(getContext());

            builder.setMessage("Calibration...")
                    .setTitle("Calibration")
                    .setCancelable(false)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            myBT.setExit(true);
                            canceled = true;
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            myBT.flush();
            myBT.clearInput();
            myBT.write("c;".toString());
            //wait for ok and put the result back
            String myMessage = "";

            myMessage = myBT.ReadResult(3000);
            if (myMessage.equals("OK")) {
                readConfig();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);
            if (!canceled) {
                // The configuration was re-read in doInBackground(): reading it again here ran
                // board I/O on the UI thread
                setAxOffsetValue(GimbalCfg.getAxOffset());
                setAyOffsetValue(GimbalCfg.getAyOffset());
                setAzOffsetValue(GimbalCfg.getAzOffset());
                setGxOffsetValue(GimbalCfg.getGxOffset());
                setGyOffsetValue(GimbalCfg.getGyOffset());
                setGzOffsetValue(GimbalCfg.getGzOffset());
                alert.dismiss();
            }
        }
    }
}