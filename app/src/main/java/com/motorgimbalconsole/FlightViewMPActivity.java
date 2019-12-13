package com.motorgimbalconsole;

import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Typeface;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
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
    String curvesNames[] = null;
    boolean[] checkedItems =null;
    XYSeriesCollection allFlightData;
    XYSeriesCollection flightData;
    ArrayList<ILineDataSet> dataSets;
    //XYPlot plot;
    //ValueAxis Xaxis;
    //ValueAxis Yaxis;
    Font font;
    //AFreeChart chart;
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
        String chartTitle= "";

        for (int i = 0; i < numberOfCurves; i++) {

            if(i < (numberOfCurves -1) )
                chartTitle = chartTitle  + flightData.getSeries(i).getKey().toString()+ "-";
            else
                chartTitle = chartTitle  + flightData.getSeries(i).getKey().toString();
        }
        //chart.setTitle(chartTitle);

        int graphBackColor;
        graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        //chart.getTitle().setFont(font);
        // set the background color for the chart...
        //chart.setBackgroundPaintType(new SolidColor(graphBackColor));

        // get a reference to the plot for further customisation...
        /*plot = chart.getXYPlot();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));

        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));*/
        int axisColor;
        axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));

        int labelColor = Color.BLACK;

        int nbrColor = Color.BLACK;
        /*Xaxis = plot.getDomainAxis();
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
        Yaxis.setTickLabelPaintType(new SolidColor(nbrColor));*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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


        mChart  = (LineChart) findViewById(R.id.linechart);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        int nbrData = flightData.getSeries(0).getItemCount();

        ArrayList<Entry> yValues = new ArrayList <>();

        for (int i = 0; i < nbrData; i++) {
            yValues.add(new Entry(flightData.getSeries(0).getX(i).longValue(), flightData.getSeries(0).getY(i).longValue()));
        }

        LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");


        set1.setDrawValues(false);
        set1.setLabel("Altitude");
        dataSets = new ArrayList<>();
        dataSets.add(set1);

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
                String currentCurvesNames[] = new String[numberOfCurves];
                for (int i = 0; i < numberOfCurves; i++) {
                    currentCurvesNames[i] = flightData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewMPActivity.this);
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
                        drawGraph();
                        dataSets.clear();
                        flightData = new XYSeriesCollection();
                        for (int i = 0; i < curvesNames.length; i++) {
                            if(checkedItems[i]) {
                                flightData.addSeries(allFlightData.getSeries(curvesNames[i]));
                               // for (int j =0; j< flightData.getSeries().size();j++) {
                                    int nbrData = allFlightData.getSeries(i).getItemCount();

                                    ArrayList<Entry> yValues = new ArrayList<>();

                                    for (int k = 0; k < nbrData; k++) {
                                        yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue(), allFlightData.getSeries(i).getY(k).floatValue()));
                                    }

                                    LineDataSet set1 = new LineDataSet(yValues, "Altitude/Time");


                                    set1.setDrawValues(false);

                                    set1.setLabel(curvesNames[i]);

                                    dataSets.add(set1);
                                //}
                            }
                        }
                        //flightData.addSeries(allFlightData.getSeries("altitude"));

                        //ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                        //plot.setDataset(0, flightData);


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
