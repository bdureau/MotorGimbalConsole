package com.motorgimbalconsole.flights;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.GlobalConfig;
import com.motorgimbalconsole.R;

import com.motorgimbalconsole.ShareHandler;
import com.motorgimbalconsole.utils.PagerFragments;
import com.motorgimbalconsole.flights.FlightView.FlightViewFcFragment;
import com.motorgimbalconsole.flights.FlightView.FlightViewInfoFragment;
import com.motorgimbalconsole.flights.FlightView.FlightViewMpFragment;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;


import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

//import static java.lang.Math.abs;

/**
 * FlightViewTabActivity.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Screen of one downloaded flight, in three tabs: curves (with the curve selection
 * dialog), statistics and CSV export (FlightViewInfoFragment), and 3D replay
 * (FlightPlayFragment). The flights only live in memory: if Android killed the process
 * meanwhile, the screen closes instead of crashing.
 */
public class FlightViewTabActivity extends AppCompatActivity {
    private FlightData myflight = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;

    private TextView[] dotsSlide;
    private LinearLayout linearDots;

    private FlightViewMpFragment flightPage1 = null;
    private FlightViewFcFragment flightPage1bis = null;
    private FlightViewInfoFragment flightPage2 = null;
    private FlightPlayFragment flightPage3 = null;

    private Button btnDismiss, butSelectCurves;
    private ConsoleApplication myBT;

    private String curvesNames[] = null;
    private String currentCurvesNames[] = null;
    private boolean[] checkedItems = null;
    private XYSeriesCollection allFlightData = null;
    private XYSeriesCollection flightData = null;

    //static Font font;
    private String FlightName = null;

    private String[] units = null;

    //public static String SELECTED_FLIGHT = "MyFlight";
    public int numberOfCurves = 0;

    @Override
    protected void attachBaseContext(Context base) {
        // Read language configuration directly from SharedPreferences to ensure
        // context localization is configured before Activity inflation
        SharedPreferences prefs = base.getSharedPreferences("GimbalConsoleCfg", MODE_PRIVATE);
        int lang = prefs.getInt("AppLanguage", 0);
        super.attachBaseContext(ConsoleApplication.updateBaseContextLocale(base, lang));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("CURRENT_CURVES_NAMES_KEY", currentCurvesNames);
        outState.putBooleanArray("CHECKED_ITEMS_KEY", checkedItems);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
        checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Storage permission, Android 9 and lower only: from Android 10 the CSV export goes
        // through MediaStore (no permission), and from Android 13 this permission can no
        // longer be granted at all, so asking for it only displayed a useless refusal.
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            int REQUEST_CODE_ASK_PERMISSIONS = 123;
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the bluetooth connection pointer
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_flight_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);

        btnDismiss = (Button) findViewById(R.id.butDismiss);
        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);

        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        this.setTitle(FlightName);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = myflight.GetFlightData(FlightName);
        if (allFlightData == null) {
            // The downloaded flights only live in memory: after Android killed the process
            // (application in background) they are gone and this screen used to crash.
            // Go back to the flight list instead; the flights can be downloaded again.
            Log.w("FlightViewTabActivity", "Flight not in memory any more: " + FlightName);
            finish();
            return;
        }

        // get a list of all the curves that have been recorded
        numberOfCurves = allFlightData.getSeries().size();
        Log.d("numberOfCurves", "numberOfCurves:" + allFlightData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
            units[i]="";
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();

        if (myBT.getAppConf().getUnits()== GlobalConfig.AltitudeUnit.METERS) {
            //Meters
            units[0] = "(" + getResources().getString(R.string.Meters_fview) + ")";
        } else {
            //Feet
            units[0] = getResources().getString(R.string.Feet_fview);
        }
        units[1] = "(°C)";
        units[2] = "(mbar)";
        units[3] ="";
        units[4] ="";
        units[5] ="";
        units[6] ="";
        units[7] ="";
        units[8] ="";
        units[9] ="";
        units[10] ="";
        // Acceleration magnitude computed from the IMU when the flight is downloaded
        if (units.length > FlightData.SERIE_ACCEL) {
            units[FlightData.SERIE_ACCEL] = "(G)";
        }

        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] = this.getResources().getString(R.string.altitude);//"altitude";
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allFlightData = null;
                finish();      //exit the application configuration activity
            }
        });

        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewTabActivity.this);

                if(curvesNames.length>0)
                    checkedItems = new boolean[curvesNames.length];
                else {
                    checkedItems = new boolean[1];
                    checkedItems[0]= true;
                }
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    if (Arrays.asList(currentCurvesNames).contains(curvesNames[i]))
                        checkedItems[i] = true;
                    else
                        checkedItems[i] = false;
                }

                builder.setMultiChoiceItems(curvesNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // The user checked or unchecked a box
                    }
                });
                // Add OK and Cancel buttons
                builder.setPositiveButton(getResources().getString(R.string.fv_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The user clicked OK
                        if ((myBT.getAppConf().getGraphicsLibType()== GlobalConfig.GraphLib.AfreeChart) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            flightPage1bis.setCheckedItems(checkedItems);
                        } else {
                            flightPage1.setCheckedItems(checkedItems);
                        }
                        int nbrOfItems =0;
                        for (int i = 0; i < checkedItems.length; i++) {
                            //count nbr of selected
                            if(checkedItems[i]) {
                                nbrOfItems++;
                            }
                        }
                        if(nbrOfItems > 0)
                            currentCurvesNames = new String[nbrOfItems];
                        else {
                            currentCurvesNames = new String[1];
                            checkedItems[0] = true;
                        }

                        int k=0;
                        for(int j=0; j< curvesNames.length; j++){
                            if(checkedItems[j]){
                                currentCurvesNames[k] = curvesNames[j];
                                k++;
                            }
                        }
                        if ((myBT.getAppConf().getGraphicsLibType()== GlobalConfig.GraphLib.AfreeChart) &
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)) {
                            flightPage1bis.drawGraph();
                            flightPage1bis.drawAllCurves(allFlightData);
                        } else {
                            flightPage1.drawGraph();
                            flightPage1.drawAllCurves(allFlightData);
                        }
                    }
                });
                //cancel
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**
     * Builds the tabs. The fragments restored by Android after a re-creation are re-used
     * (see PagerFragments): the curve selection dialog must update the displayed chart.
     * New fragments receive the flight name through their arguments (newInstance).
     */
    private void setupViewPager(ViewPager viewPager) {
        FragmentManager fm = getSupportFragmentManager();
        adapter = new SectionsPageAdapter(fm);

        //if we are using afreeChart
        if ((myBT.getAppConf().getGraphicsLibType()== GlobalConfig.GraphLib.AfreeChart) &
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)){
            flightPage1bis = PagerFragments.restoredOr(fm, FlightViewFcFragment.class,
                    () -> FlightViewFcFragment.newInstance(FlightName, curvesNames, checkedItems, units));
            adapter.addFragment(flightPage1bis, "TAB1");
        } else {
            flightPage1 = PagerFragments.restoredOr(fm, FlightViewMpFragment.class,
                    () -> FlightViewMpFragment.newInstance(FlightName, curvesNames, checkedItems, units));
            adapter.addFragment(flightPage1, "TAB1");
        }
        flightPage2 = PagerFragments.restoredOr(fm, FlightViewInfoFragment.class,
                () -> FlightViewInfoFragment.newInstance(FlightName, units));

        adapter.addFragment(flightPage2, "TAB2");

        flightPage3 = PagerFragments.restoredOr(fm, FlightPlayFragment.class,
                () -> FlightPlayFragment.newInstance(FlightName));

        adapter.addFragment(flightPage3, "TAB3");

        linearDots=findViewById(R.id.idFlightLinearDots);
        agregaIndicateDots(0, adapter.getCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(viewListener);
    }

    public void agregaIndicateDots(int pos, int nbr){
        dotsSlide =new TextView[nbr];
        linearDots.removeAllViews();

        for (int i=0; i< dotsSlide.length; i++){
            dotsSlide[i]=new TextView(this);
            dotsSlide[i].setText(Html.fromHtml("&#8226;"));
            dotsSlide[i].setTextSize(35);
            dotsSlide[i].setTextColor(getResources().getColor(R.color.colorWhiteTransparent));
            linearDots.addView(dotsSlide[i]);
        }

        if(dotsSlide.length>0){
            dotsSlide[pos].setTextColor(getResources().getColor(R.color.colorWhite));
        }
    }

    ViewPager.OnPageChangeListener viewListener=new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            agregaIndicateDots(i, adapter.getCount());
            if(i ==0) {
                butSelectCurves.setVisibility(View.VISIBLE);
            } else {
                butSelectCurves.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }
/*
    private  void takeScreenShot(View view) {
        Date date = new Date();
        CharSequence format = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date);

        try {
            File mainDir = new File(
                    this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilShare");
            if (!mainDir.exists()) {
                boolean mkdir = mainDir.mkdir();
            }

            String path = mainDir + "/" + "GimbalCurve" + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);


            File imageFile = new File(path);
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            shareScreenShot(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Share ScreenShot
    private  void shareScreenShot(File imageFile ) {

        Log.d("Package Name", "Package Name" + this.getPackageName());
        Uri uri = FileProvider.getUriForFile(
                this,
                this.getPackageName() +  ".provider",
                imageFile);


        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Gimbal has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        try {
            this.startActivity(Intent.createChooser(intent, "Share With"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_flights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //open application settings screen
        if (id == R.id.action_share) {
            //takeScreenShot(findViewById(android.R.id.content).getRootView());
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_flight");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}