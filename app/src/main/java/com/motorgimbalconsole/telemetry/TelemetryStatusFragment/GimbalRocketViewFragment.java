package com.motorgimbalconsole.telemetry.TelemetryStatusFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.R;
import com.motorgimbalconsole.utils.Rocket;

import processing.android.PFragment;
import processing.core.PApplet;
/*
    This is the third tab
    it displays the rocket orientation
     */
public class GimbalRocketViewFragment extends Fragment {
    private static final String TAG = "Tab3StatusFragment";
    private PApplet myRocket;
    boolean ViewCreated = false;
    private PFragment fragment;
    private View view;
    public boolean isViewCreated() {
        return ViewCreated;
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_gimbal_status_part3,container,false);

        myRocket = new Rocket();
        fragment = new PFragment(myRocket);

        getChildFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        ViewCreated = true;
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragment.onDestroy();
        myRocket.onDestroy();
    }
    @Override
    public void onStop() {

        Log.d(TAG, "onStop");
        super.onStop();
        fragment.onStop();
        myRocket.onStop();

    }
    @Override
    public void onStart() {
        super.onStart();
        fragment.requestDraw();
        view.refreshDrawableState();
        view.bringToFront();
    }
    @Override
    public void onResume() {
        super.onResume();
        fragment.onResume(); //perahps we can remove that
        //This is the only way I can redraw the rocket after leaving the tab
        getChildFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
    }
    //send the quaternion to the processing widget
    public void setInputString(String value) {
        //if (ViewCreated)
        if(view != null)
            ((Rocket) myRocket).setInputString(value);
    }
    public void setInputCorrect(String value) {
        if(view != null)
            ((Rocket) myRocket).setInputCorrect(value);
    }
    public void setServoX(String value) {
        if(view != null)
            ((Rocket) myRocket).setServoX(value);
    }
    public void setServoY(String value) {
        if(view != null)
            ((Rocket) myRocket).setServoY(value);
    }
}
