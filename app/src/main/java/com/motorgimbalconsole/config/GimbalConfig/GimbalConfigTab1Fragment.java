package com.motorgimbalconsole.config.GimbalConfig;

import static com.motorgimbalconsole.config.GimbalTabConfigActivity.readConfig;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
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

public class GimbalConfigTab1Fragment extends Fragment {
    private static final String TAG = "Tab1Fragment";
    private boolean ViewCreated = false;
    private EditText txtViewAxOffset, txtViewAyOffset, txtViewAzOffset;
    private EditText txtViewGxOffset, txtViewGyOffset, txtViewGzOffset;
    private Button btnCalibrate;
    ConsoleApplication myBT;
    private GimbalConfigData GimbalCfg = null;

    public GimbalConfigTab1Fragment(ConsoleApplication pBT,
                                    GimbalConfigData pGimbalCfg
                                    ) {
        myBT = pBT;
        GimbalCfg = pGimbalCfg;
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

    public boolean isViewCreated() {
        return ViewCreated;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tabconfigpart1_fragment, container, false);
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
    private class Calibration extends AsyncTask<Void, Void, Void>  // UI thread
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
                readConfig();
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