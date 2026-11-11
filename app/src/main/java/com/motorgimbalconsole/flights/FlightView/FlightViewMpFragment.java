package com.motorgimbalconsole.flights.FlightView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;

import org.afree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

/**
 * FlightViewMpFragment.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: Curves tab of a downloaded flight drawn with MPAndroidChart (default library):
 * plots the curves chosen in the selection dialog of FlightViewTabActivity. Created with
 * newInstance(); the flight is read from the application FlightData by its name.
 */
public class FlightViewMpFragment extends Fragment {
    private LineChart mChart;
    public XYSeriesCollection allFlightData;
    private ConsoleApplication myBT;
    int graphBackColor, fontSize, axisColor, labelColor, nbrColor;
    private ArrayList<ILineDataSet> dataSets;

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
    public FlightViewMpFragment() {
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
    public static FlightViewMpFragment newInstance(String flightName, String[] curvesNames,
                                           boolean[] checkedItems, String[] units) {
        FlightViewMpFragment fragment = new FlightViewMpFragment();
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

        View view = inflater.inflate(R.layout.fragment_flightview_mp, container, false);

        mChart = (LineChart) view.findViewById(R.id.linechart);


        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
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
    }

    public void drawAllCurves(XYSeriesCollection allFlightData) {
        // Nothing to draw (flight no longer in memory, or no sample): getX(nbrOfItem - 1)
        // below would throw
        if (allFlightData == null || allFlightData.getSeriesCount() == 0
                || allFlightData.getSeries(0).getItemCount() == 0) {
            return;
        }
        dataSets = new ArrayList<>();
        dataSets.clear();
        String graphTimeUnits = getResources().getString(R.string.unit_time);
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

                int nbrData = allFlightData.getSeries(i).getItemCount();

                ArrayList<Entry> yValues = new ArrayList<>();

                for (int k = 0; k < nbrData; k++) {
                    yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue()*timeFactor, allFlightData.getSeries(i).getY(k).floatValue()));
                }

                LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.Altitude_time));
                set1.setColor(colors[i]);

                set1.setDrawValues(false);
                set1.setDrawCircles(false);
                set1.setLabel(curvesNames[i] +" "+units[i]);
                set1.setValueTextColor(labelColor);
                set1.setValueTextSize(fontSize);
                dataSets.add(set1);
            }
        }

        LineData data = new LineData(dataSets);
        mChart.clear();
        mChart.setData(data);
        mChart.setBackgroundColor(graphBackColor);
        Description desc = new Description();
        //time (ms)
        desc.setText(graphTimeUnits);
        mChart.setDescription(desc);
    }
}
