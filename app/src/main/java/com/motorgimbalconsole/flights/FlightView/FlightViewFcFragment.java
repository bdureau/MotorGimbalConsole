package com.motorgimbalconsole.flights.FlightView;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import org.afree.chart.AFreeChart;
import org.afree.chart.ChartFactory;
import org.afree.chart.axis.NumberAxis;
import org.afree.chart.axis.ValueAxis;
import org.afree.chart.plot.PlotOrientation;
import org.afree.chart.plot.XYPlot;
import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.SolidColor;
import org.afree.graphics.geom.Font;

/**
 * FlightViewFcFragment.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Curves tab of a downloaded flight drawn with AFreeChart (Android 7 and lower when
 * this library is selected): plots the curves chosen in the selection dialog of
 * FlightViewTabActivity. Created with newInstance(); the flight is read from the application
 * FlightData by its name.
 */
public class FlightViewFcFragment extends Fragment {
    private ChartView chartView;
    private AFreeChart mChart = null;
    private XYPlot plot;
    public XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;

    private String units[];
    private String curvesNames[];
    boolean checkedItems[];

    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};

    // Keys of the arguments Bundle (survives the re-creation of the fragment by Android)
    private static final String ARG_FLIGHT_NAME = "flightName";
    private static final String ARG_CURVES_NAMES = "curvesNames";
    private static final String ARG_CHECKED_ITEMS = "checkedItems";
    private static final String ARG_UNITS = "units";
    /** Key of the saved curve selection (changes after the fragment was created). */
    private static final String STATE_CHECKED_ITEMS = "stateCheckedItems";

    /**
     * No-argument constructor, required by Android to re-create the fragment (rotation,
     * process restore). Use {@link #newInstance} to create it.
     */
    public FlightViewFcFragment() {
    }

    /**
     * Creates the curves tab of a downloaded flight. The parameters are stored in the
     * arguments Bundle instead of fields set by a constructor, which were lost when Android
     * re-created the fragment (the tab then crashed or stayed empty).
     *
     * @param flightName   name of the flight in the application FlightData
     * @param curvesNames  names of all the curves of the flight
     * @param checkedItems curves initially displayed
     * @param units        unit label of each curve
     * @return the fragment
     */
    public static FlightViewFcFragment newInstance(String flightName, String[] curvesNames,
                                           boolean[] checkedItems, String[] units) {
        FlightViewFcFragment fragment = new FlightViewFcFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FLIGHT_NAME, flightName);
        args.putStringArray(ARG_CURVES_NAMES, curvesNames);
        args.putBooleanArray(ARG_CHECKED_ITEMS, checkedItems);
        args.putStringArray(ARG_UNITS, units);
        fragment.setArguments(args);
        // Also usable right away, before onCreate (the activity may call drawAllCurves)
        fragment.readArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readArguments(getArguments());
        if (savedInstanceState != null) {
            boolean[] saved = savedInstanceState.getBooleanArray(STATE_CHECKED_ITEMS);
            if (saved != null) checkedItems = saved;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(STATE_CHECKED_ITEMS, checkedItems);
    }

    /** Restores the fields from the arguments Bundle; the flight comes from the application. */
    private void readArguments(@Nullable Bundle args) {
        myBT = ConsoleApplication.getInstance();
        if (args == null) return;
        curvesNames = args.getStringArray(ARG_CURVES_NAMES);
        checkedItems = args.getBooleanArray(ARG_CHECKED_ITEMS);
        units = args.getStringArray(ARG_UNITS);
        String flightName = args.getString(ARG_FLIGHT_NAME);
        if (flightName != null && myBT != null) {
            allFlightData = myBT.getFlightData().GetFlightData(flightName);
        }
    }

    public void setCheckedItems(boolean[] checkedItems) {
        this.checkedItems = checkedItems;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_flightview_fc, container, false);

        chartView = (ChartView) view.findViewById(R.id.chartView1);

        mChart = ChartFactory.createXYLineChart(
                "", //getResources().getString(R.string.Altitude_time),
                getResources().getString(R.string.Time_fv),
                "",//getResources().getString(R.string.Altitude) + " (" + myUnits + ")",
                null,
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                true,                     // tooltips?
                false                     // URLs?
        );

        chartView.setChart(mChart);
        drawGraph();
        drawAllCurves(allFlightData);

        return view;
    }

    public void drawGraph() {
        graphBackColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphBackColor());
        fontSize = myBT.getAppConf().ConvertFont(myBT.getAppConf().getFontSize());
        axisColor = myBT.getAppConf().ConvertColor(myBT.getAppConf().getGraphColor());
        labelColor = Color.BLACK;
        nbrColor = Color.BLACK;
        //font
        Font font = new Font("Dialog", Typeface.NORMAL,fontSize);
        mChart.getTitle().setFont(font);

        // set the background color for the chart...
        mChart.setBackgroundPaintType(new SolidColor(graphBackColor));
        // get a reference to the plot for further customisation...
        plot = mChart.getXYPlot();
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setBackgroundPaintType(new SolidColor(graphBackColor));
        plot.setOutlinePaintType(new SolidColor(Color.YELLOW));
        plot.setDomainZeroBaselinePaintType(new SolidColor(Color.GREEN));
        plot.setRangeZeroBaselinePaintType(new SolidColor(Color.MAGENTA));
        final ValueAxis Xaxis = plot.getDomainAxis();
        Xaxis.setAutoRange(true);
        Xaxis.setAxisLinePaintType(new SolidColor(axisColor));
        Xaxis.setLabel("toto");

        final ValueAxis YAxis = plot.getRangeAxis();
        YAxis.setAxisLinePaintType(new SolidColor(axisColor));


        Xaxis.setTickLabelFont(font);
        Xaxis.setLabelFont(font);

        YAxis.setTickLabelFont(font);
        YAxis.setLabelFont(font);

        //X axis label color
        Xaxis.setLabelPaintType(new SolidColor(labelColor));
        Xaxis.setTickMarkPaintType(new SolidColor(axisColor));
        Xaxis.setTickLabelPaintType(new SolidColor(nbrColor));
        //Y axis label color
        YAxis.setLabelPaintType(new SolidColor(labelColor));
        YAxis.setTickLabelPaintType(new SolidColor(nbrColor));
        final NumberAxis rangeAxis2 = new NumberAxis("Range Axis 2");
        rangeAxis2.setAutoRangeIncludesZero(false);
    }

    public void drawAllCurves(XYSeriesCollection allFlightData) {
        // Nothing to draw (flight no longer in memory, or no sample): getX(nbrOfItem - 1)
        // below would throw
        if (allFlightData == null || allFlightData.getSeriesCount() == 0
                || allFlightData.getSeries(0).getItemCount() == 0) {
            return;
        }
        XYSeriesCollection flightData;
        flightData = new XYSeriesCollection();
        String graphTimeUnits = getResources().getString(R.string.unit_time);
        String graphTitle="";

        int nbrOfItem = allFlightData.getSeries(0).getItemCount();
        float maxTime = allFlightData.getSeries(0).getX(nbrOfItem-1).floatValue();

        float timeFactor = 1;
        if(maxTime > 10000){
            graphTimeUnits = getString(R.string.unit_time_seconde);
            timeFactor =0.001f;
        }

        if(maxTime > 600000){
            graphTimeUnits = getString(R.string.unit_time_minutes);
            timeFactor =0.001f/60;
        }

        for (int i = 0; i < curvesNames.length; i++) {

            if (checkedItems[i]) {
                XYSeries currentCurve = new XYSeries(0);
                for (int k = 0; k < nbrOfItem; k++) {
                    currentCurve.add(allFlightData.getSeries(i).getX(k).floatValue()*timeFactor, (allFlightData.getSeries(i).getY(k).floatValue() ));
                }
                flightData.addSeries(currentCurve);
                graphTitle = graphTitle + curvesNames[i] + " " + units[i];
            }
        }
        plot.setDataset(0, flightData);
        mChart.setTitle(graphTitle);
        //plot time units
        plot.getDomainAxis().setLabel(graphTimeUnits);
    }
}

