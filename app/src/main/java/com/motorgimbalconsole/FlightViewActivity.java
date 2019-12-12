package com.motorgimbalconsole;

import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Typeface;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;

import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;

import java.util.Arrays;


import static android.view.View.*;

/**
 * @description: This will display each flight. You can select which curve you want to display.
 * You can also  play back the flight
 * @author: boris.dureau@neuf.fr
 **/

public class FlightViewActivity extends AppCompatActivity {
    String FlightName = null;
    ConsoleApplication myBT;
    private FlightData myflight = null;
    private Button buttonDismiss, butSelectCurves, btnPlay;
    String curvesNames[] = null;
    boolean[] checkedItems =null;
    XYSeriesCollection allFlightData;
    XYSeriesCollection flightData;
    XYPlot plot;
    ValueAxis Xaxis;
    ValueAxis Yaxis;
    Font font;
    AFreeChart chart;
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
        String chartTitle= "";
        //String currentCurvesNames[] = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            //currentCurvesNames[i] = flightData.getSeries(i).getKey().toString();
            if(i < (numberOfCurves -1) )
                chartTitle = chartTitle  + flightData.getSeries(i).getKey().toString()+ "-";
            else
                chartTitle = chartTitle  + flightData.getSeries(i).getKey().toString();
        }
        chart.setTitle(chartTitle);

        int graphBackColor;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.getTitle().setFont(font);
        // set the background color for the chart...
        chart.setBackgroundPaintType(new SolidColor(graphBackColor));

        // get a reference to the plot for further customisation...
        plot = chart.getXYPlot();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));

        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));
        int axisColor;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));

        Yaxis = plot.getRangeAxis();

        Yaxis.setAxisLinePaintType(new SolidColor(axisColor));


        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        Yaxis.setTickLabelFont(font);
        Yaxis.setLabelFont(font);

        //Xaxis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));

        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        Yaxis.setLabelPaintType(new SolidColor(labelColor));
        Yaxis.setTickLabelPaintType(new SolidColor(nbrColor));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //get the bluetooth Application pointer
        myBT = (ConsoleApplication) getApplication();
        //Check the local and force it if needed
        getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view);
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

        chart = ChartFactory.createXYLineChart(
                "", //getResources().getString(R.string.Altitude_time),
                getResources().getString(R.string.Time_fv),
                "",//getResources().getString(R.string.Altitude) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );
        drawGraph();

        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);


        plot.setDataset(0, flightData);


        ChartView chartView = (ChartView) findViewById(R.id.chartView1);
        chartView.setChart(chart);
        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });

        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(FlightViewActivity.this, PlayFlight.class);
                i.putExtra(SELECTED_FLIGHT, FlightName);
                startActivity(i);
            }
        });
        butSelectCurves.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int numberOfCurves = flightData.getSeries().size();
                String currentCurvesNames[] = new String[numberOfCurves];
                for (int i = 0; i < numberOfCurves; i++) {
                    currentCurvesNames[i] = flightData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewActivity.this);
                builder.setTitle(getResources().getString(R.string.flight_data_title));
                checkedItems = new boolean[curvesNames.length];
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    if(Arrays.asList(currentCurvesNames).contains(curvesNames[i]))
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
                        flightData = new XYSeriesCollection();
                        for (int i = 0; i < curvesNames.length; i++) {
                            if(checkedItems[i]) {
                                flightData.addSeries(allFlightData.getSeries(curvesNames[i]));
                            }
                        }
                        //flightData.addSeries(allFlightData.getSeries("altitude"));
                        drawGraph();

                        plot.setDataset(0, flightData);
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
