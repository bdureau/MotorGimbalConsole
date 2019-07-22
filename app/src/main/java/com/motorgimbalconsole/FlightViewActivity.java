package com.motorgimbalconsole;

import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.afree.chart.ChartFactory;
import org.afree.chart.AFreeChart;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;
import org.afree.data.category.DefaultCategoryDataset;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;

import java.util.List;

import processing.core.PShapeSVG;

import static android.view.View.*;

/**
 * @description: This will display each flight
 * @author: boris.dureau@neuf.fr
 **/

public class FlightViewActivity extends AppCompatActivity {
    String FlightName = null;
    ConsoleApplication myBT;
    private FlightData myflight = null;
    private Button buttonDismiss, butSelectCurves;
    String curvesNames[] = null;
    boolean[] checkedItems =null;
    XYSeriesCollection allFlightData;
    XYSeriesCollection flightData;
    XYPlot plot;

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
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
        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);

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


        //msg(allFlightData.getSeries(0).getKey().toString());


        //DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Read the application config
        myBT.getAppConf().ReadConfig();
        int graphBackColor;//= Color.WHITE;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        int fontSize;
        fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));

        int axisColor;//=Color.BLACK;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        String myUnits = "";
        if (myBT.getAppConf().getUnits().equals("0"))
            //Meters
            myUnits = getResources().getString(R.string.Meters_fview);
        else
            //Feet
            myUnits = getResources().getString(R.string.Feet_fview);

        //font
        Font font = new Font("Dialog", Typeface.NORMAL, fontSize);

        AFreeChart chart = ChartFactory.createXYLineChart(
                getResources().getString(R.string.Altitude_time),
                getResources().getString(R.string.Time_fv),
                getResources().getString(R.string.Altitude) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );

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
        //plot.setRangeTickBandPaintType(new SolidColor(Color.GREEN)); //rajoute des bandes
        //plot.setOutlinePaintType(new SolidColor(Color.MAGENTA));
        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));

        final ValueAxis Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));

        final ValueAxis YAxis = plot.getRangeAxis();
        YAxis.setAxisLinePaintType(new SolidColor(axisColor));


        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        YAxis.setTickLabelFont(font);
        YAxis.setLabelFont(font);

        //Xaxis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));
     /*this make it crash
     axis.setAutoRangeMinimumSize(0);
     rangeAxis.setAutoRangeMinimumSize(0);*/
        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);

        //plot.setLabelFont(new Font("SansSerif", Typeface.NORMAL, 8));
        //plot.
        // plot.setCircular(false);
        //plot.setLabelLinksVisible(false);


        plot.setDataset(0, flightData);


        ChartView chartView = (ChartView) findViewById(R.id.chartView1);
        chartView.setChart(chart);
        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();      //exit the activity
            }
        });


        butSelectCurves.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewActivity.this);
                builder.setTitle("Select flight curve");
                checkedItems = new boolean[curvesNames.length];
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    checkedItems[i] = false;
                }
                // boolean[] checkedItems = {true, false, false, true, false};

                builder.setMultiChoiceItems(curvesNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // The user checked or unchecked a box
                    }
                });
                // Add OK and Cancel buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
                        plot.setDataset(0, flightData);
                    }
                });
                builder.setNegativeButton("Cancel", null);

// Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }


        });
    }

}
