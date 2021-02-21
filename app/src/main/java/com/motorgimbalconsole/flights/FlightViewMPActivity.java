package com.motorgimbalconsole.flights;

import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Typeface;

//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;


import org.afree.data.xy.XYSeriesCollection;

import org.afree.graphics.geom.Font;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import java.util.ArrayList;
import java.util.Arrays;


import static android.view.View.*;

/**
 * @description: This will display each flight. You can select which curve you want to display.
 * You can also  play back the flight
 * @author: boris.dureau@neuf.fr
 **/

public class FlightViewMPActivity extends AppCompatActivity {
    String FlightName = null;
    ConsoleApplication myBT;
    private FlightData myflight = null;
    private Button buttonDismiss, butSelectCurves, btnPlay;
    private String curvesNames[] = null;
    private String currentCurvesNames[] =null;
    boolean[] checkedItems = null;
    XYSeriesCollection allFlightData;
    XYSeriesCollection flightData;
    ArrayList<ILineDataSet> dataSets;
    int colors []= {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW,Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    Font font;

    private LineChart mChart;
    public static String SELECTED_FLIGHT = "MyFlight";

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void drawGraph() {

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        String myUnits = "";
        if (myBT.getAppConf().getUnits().equals("0"))
            //Meters
            myUnits = getResources().getString(R.string.Meters_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);

        //font
        font = new Font("Dialog", Typeface.NORMAL, fontSize);

        int numberOfCurves = flightData.getSeries().size();
        String chartTitle = "";

        for (int i = 0; i < numberOfCurves; i++) {

            if (i < (numberOfCurves - 1))
                chartTitle = chartTitle + flightData.getSeries(i).getKey().toString() + "-";
            else
                chartTitle = chartTitle + flightData.getSeries(i).getKey().toString();
        }
        //chart.setTitle(chartTitle);

        int graphBackColor;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));


        int axisColor;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;

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
        outState.putBooleanArray("CHECKED_ITEMS_KEY",checkedItems);

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

        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view_mp);
        buttonDismiss = (Button) findViewById(R.id.butDismiss);
        butSelectCurves = (Button) findViewById(R.id.butSelectCuves);
        btnPlay = (Button) findViewById(R.id.butPlay);
        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        this.setTitle(FlightName);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = myflight.GetFlightData(FlightName);

        // by default we will display the altitude
        // but then the user will be able to change the data
        flightData = new XYSeriesCollection();
        flightData.addSeries(allFlightData.getSeries("altitude"));

        // get a list of all the curves that have been recorded
        //List allCurves = allFlightData.getSeries();
        int numberOfCurves = allFlightData.getSeries().size();
        curvesNames = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();


        drawGraph();

        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] ="altitude";
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        mChart = (LineChart) findViewById(R.id.linechart);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        /*int nbrData = flightData.getSeries(0).getItemCount();

        ArrayList<Entry> yValues = new ArrayList<>();

        for (int i = 0; i < nbrData; i++) {
            yValues.add(new Entry(flightData.getSeries(0).getX(i).longValue(), flightData.getSeries(0).getY(i).longValue()));
        }*/
        dataSets = new ArrayList<>();
        for (int i = 0; i < curvesNames.length; i++) {
            if (checkedItems[i]) {
                flightData.addSeries(allFlightData.getSeries(curvesNames[i]));

                int nbrData = allFlightData.getSeries(i).getItemCount();

                ArrayList<Entry> yValues = new ArrayList<>();

                for (int k = 0; k < nbrData; k++) {
                    yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue(), allFlightData.getSeries(i).getY(k).floatValue()));
                }

                LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");
                set1.setColor(colors[i]);

                set1.setDrawValues(false);
                set1.setDrawCircles(false);
                set1.setLabel(curvesNames[i]);

                dataSets.add(set1);


            }
        }

        /*LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");

        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setLabel("Altitude");*/
        //dataSets = new ArrayList<>();
        //dataSets.add(set1);

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        Description desc = new Description();
        desc.setText("test");
        mChart.setDescription(desc);

        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });

        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(FlightViewMPActivity.this, PlayFlight.class);
                i.putExtra(SELECTED_FLIGHT, FlightName);
                startActivity(i);
            }
        });
        butSelectCurves.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int numberOfCurves = flightData.getSeries().size();
                currentCurvesNames = new String[numberOfCurves];

                for (int i = 0; i < numberOfCurves; i++) {
                    currentCurvesNames[i] = flightData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewMPActivity.this);
                builder.setTitle(getResources().getString(R.string.flight_data_title));
                checkedItems = new boolean[curvesNames.length];
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
                        drawGraph();
                        dataSets.clear();

                        flightData = new XYSeriesCollection();
                        for (int i = 0; i < curvesNames.length; i++) {
                            if (checkedItems[i]) {
                                flightData.addSeries(allFlightData.getSeries(curvesNames[i]));

                                int nbrData = allFlightData.getSeries(i).getItemCount();

                                ArrayList<Entry> yValues = new ArrayList<>();

                                for (int k = 0; k < nbrData; k++) {
                                    yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue(), allFlightData.getSeries(i).getY(k).floatValue()));
                                }

                                LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");
                                set1.setColor(colors[i]);

                                set1.setDrawValues(false);
                                set1.setDrawCircles(false);
                                set1.setLabel(curvesNames[i]);

                                dataSets.add(set1);


                            }
                        }

                        LineData data = new LineData(dataSets);
                        mChart.clear();
                        mChart.setData(data);

                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }


        });
    }

}
