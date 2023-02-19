package com.motorgimbalconsole.flights.FlightView;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.flights.FlightData;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FlightViewInfoFragment extends Fragment {

    private FlightData myflight;
    XYSeriesCollection allFlightData;
    private TextView nbrOfSamplesValue, flightNbrValue;
    private TextView apogeeAltitudeValue, flightDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;
    private TextView timeToApogeeValue, mainAltitudeValue, maxDescentValue, landingSpeedValue;
    private double FEET_IN_METER = 1;

    private AlertDialog.Builder builder = null;
    private AlertDialog alert;

    String SavedCurves = "";
    private String units[];
    private String FlightName;
    private ConsoleApplication myBT;
    private XYSeries altitude;
    private XYSeries speed;
    private XYSeries accel;
    private int numberOfCurves = 0;

    public FlightViewInfoFragment(FlightData data,
                                  XYSeriesCollection data2,
                                  ConsoleApplication BT,
                                  String pUnits[],
                                  String pFlightName) {

        myflight = data;
        this.allFlightData = data2;
        this.units = pUnits;
        this.FlightName = pFlightName;
        this.myBT = BT;
    }

    public void msg(String s) {
        Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private Button buttonExportToCsv, butShareFiles;
    int nbrSeries;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_flightview_info, container, false);

        buttonExportToCsv = (Button) view.findViewById(R.id.butExportToCsv);
        butShareFiles = (Button) view.findViewById(R.id.butShareFiles);
        apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
        flightDurationValue = view.findViewById(R.id.flightDurationValue);
        burnTimeValue = view.findViewById(R.id.burnTimeValue);
        maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
        maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
        timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
        mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
        maxDescentValue = view.findViewById(R.id.maxDescentValue);
        landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
        nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
        flightNbrValue = view.findViewById(R.id.flightNbrValue);

        XYSeriesCollection flightData;

        flightData = myflight.GetFlightData(FlightName);
        int nbrData = flightData.getSeries(0).getItemCount();
        nbrSeries = flightData.getSeriesCount();

        altitude = allFlightData.getSeries(getResources().getString(R.string.altitude));
        //get speed
        speed = allFlightData.getSeries(getResources().getString(R.string.curve_speed));
        // get acceleration
        accel = allFlightData.getSeries(getResources().getString(R.string.curve_accel));

        // flight nbr
        flightNbrValue.setText(FlightName + "");

        //nbr of samples
        nbrOfSamplesValue.setText(nbrData + "");

        //flight duration
        double flightDuration = flightData.getSeries(0).getMaxX() / 1000;
        flightDurationValue.setText(String.format("%.2f",flightDuration) + " secs");
        //apogee altitude
        double apogeeAltitude = flightData.getSeries(0).getMaxY();
        apogeeAltitudeValue.setText(String.format("%.0f",apogeeAltitude) + " " + myBT.getAppConf().getUnitsValue());

        //apogee time
        int pos = searchX(flightData.getSeries(0), apogeeAltitude);
        double apogeeTime = (double) flightData.getSeries(0).getX(pos);
        timeToApogeeValue.setText(String.format("%.2f",apogeeTime / 1000) + " secs");

        //calculate max speed
        double maxSpeed = speed.getMaxY();
        maxVelociyValue.setText((long) maxSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");

        //landing speed
        double landingSpeed = 0;
        int timeBeforeLanding =searchXBack(altitude, 30);
        if (timeBeforeLanding != -1)
            if (searchY(speed, altitude.getX(timeBeforeLanding).doubleValue() )!= -1) {
                landingSpeed =searchY(speed, altitude.getX(timeBeforeLanding).doubleValue() );
                landingSpeedValue.setText((long) landingSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                landingSpeedValue.setText("N/A");
            }
            /*if (searchY(speed, flightData.getSeries(0).getMaxX() - 2000) != -1) {
                landingSpeed = speed.getY(searchY(speed, flightData.getSeries(0).getMaxX() - 2000)).doubleValue();
                landingSpeedValue.setText((long) landingSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                landingSpeedValue.setText("N/A");
            }*/
        //max descente speed
        double maxDescentSpeed = 0;
        //int timeBeforeLanding =searchXBack(altitude, 30);

            /*if (searchY(speed, timeBeforeLanding) != -1) {
                maxDescentSpeed = speed.getY(searchY(speed, timeBeforeLanding)).doubleValue();
                maxDescentValue.setText((long) maxDescentSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                maxDescentValue.setText("N/A");
            }*/
           /* if (searchY(speed, apogeeTime + 2000) != -1) {
                maxDescentSpeed = speed.getY(searchY(speed, apogeeTime + 2000)).doubleValue();
                maxDescentValue.setText((long) maxDescentSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                maxDescentValue.setText("N/A");
            }*/
        if (searchY(speed, apogeeTime + 100) != -1) {
            int pos1 = searchY(speed, apogeeTime + 100);
            XYSeries partialSpeed;
            partialSpeed = new XYSeries("partial_speed");
            for(int i = pos1; i < speed.getItemCount(); i++ ) {
                partialSpeed.add(speed.getX(i), speed.getY(i));
            }
            maxDescentSpeed = partialSpeed.getMaxY();
            //maxDescentSpeed = speed.getY(searchY(speed, apogeeTime + 500)).doubleValue();
            maxDescentValue.setText((long) maxDescentSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");
        } else {
            maxDescentValue.setText("N/A");
        }

        //max acceleration value
        double maxAccel = accel.getMaxY();
        maxAccel = (maxAccel * FEET_IN_METER) / 9.80665;

        maxAccelerationValue.setText((long) maxAccel + " G");

        //burntime value
        double burnTime = 0;
        if (searchX(speed, maxSpeed) != -1)
            burnTime = speed.getX(searchX(speed, maxSpeed)).doubleValue();
        if (burnTime != 0)
            burnTimeValue.setText(String.format("%.2f",burnTime / 1000) + " secs");
        else
            burnTimeValue.setText("N/A");
        //main value
        // remain TODO!!!
        mainAltitudeValue.setText(" " + myBT.getAppConf().getUnitsValue());

        buttonExportToCsv.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SavedCurves = "";
                //export the data to a csv file
                for (int j = 0; j < numberOfCurves; j++) {
                    Log.d("Flight win", "Saving curve:" + j);
                    saveData(j, allFlightData);
                }
                builder = new AlertDialog.Builder(getContext());
                //Running Saving commands
                builder.setMessage(getResources().getString(R.string.save_curve_msg) + Environment.DIRECTORY_DOWNLOADS + "\\MotorGimbalConsoleFlights \n" + SavedCurves)
                        .setTitle(getResources().getString(R.string.save_curves_title))
                        .setCancelable(false)
                        .setPositiveButton(R.string.save_curve_ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });

                alert = builder.create();
                alert.show();
                msg(getResources().getString(R.string.curves_saved_msg));

            }
        });

        butShareFiles.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SavedCurves = "";
                ArrayList<String> fileNames = new ArrayList<>();
                // Create a file for the zip file
                File zipFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "gimbalData.zip");

                for (int j = 0; j < numberOfCurves; j++) {
                    Log.d("Flight win", "Saving curve:" + j);
                    //export the data to a csv file
                    String fileName = saveData(j, allFlightData);
                    fileNames.add(fileName);
                    Log.d("Flight win", "Saving curve name :" + fileName);
                }
                try {
                    // Create a zip output stream to write to the zip file
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    for (String fileName : fileNames) {
                        ZipEntry ze = new ZipEntry(fileName);
                        // Add the zip entry to the zip output stream
                        zos.putNextEntry(ze);
                        // Read the file and write it to the zip output stream
                        File filetoZip = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),  fileName);
                        FileInputStream fis = new FileInputStream(filetoZip);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        // Close the zip entry and the file input stream
                        zos.closeEntry();
                        fis.close();
                    }
                    // Close the zip output stream
                    zos.close();
                    fos.close();
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.d("error", "we have an issue");
                }

                //Toast.makeText(getContext(), currentEng, Toast.LENGTH_SHORT).show();
                shareFile(zipFile);
            }
        });
        return view;
    }

    private String saveData(int nbr, XYSeriesCollection Data) {
        String fileName ="";
        String valHeader = "";

        if (nbr == 0) {
            valHeader = getResources().getString(R.string.curve_altitude);
        } else if (nbr == 1) {
            valHeader = getResources().getString(R.string.curve_temperature);
        } else if (nbr == 2) {
            valHeader = getResources().getString(R.string.curve_pressure);
        } else if (nbr == 3) {
            valHeader = "Gravity X";
        }else if (nbr == 4) {
            valHeader = "Gravity Y";
        }else if (nbr == 5) {
            valHeader = "Gravity Z";
        }else if (nbr == 6) {
            valHeader = "Euler X";
        }else if (nbr == 7) {
            valHeader = "Euler Y";
        }else if (nbr == 8) {
            valHeader = "Euler Z";
        }else if (nbr == 9) {
            valHeader = "Yaw";
        }else if (nbr == 10) {
            valHeader = "Pitch";
        }else if (nbr == 11) {
            valHeader = "Roll";
        }else if (nbr == 12) {
            valHeader = "outputX";
        }else if (nbr == 13) {
            valHeader = "outputY";
        }else if (nbr == 14) {
            valHeader = "accelX";
        }else if (nbr == 15) {
            valHeader = "accelY";
        }else if (nbr == 16) {
            valHeader = "accelZ";
        }else if (nbr == 17) {
            valHeader = getResources().getString(R.string.curve_speed);
        } else if (nbr == 18) {
            valHeader = getResources().getString(R.string.curve_accel);
        }

        String csv_data = "time(ms),"+valHeader + " " + units[nbr] +"\n";/// your csv data as string;
        int nbrData = Data.getSeries(nbr).getItemCount();
        for (int i = 0; i < nbrData; i++) {
            csv_data = csv_data + (double) Data.getSeries(nbr).getX(i) + "," + (double) Data.getSeries(nbr).getY(i) + "\n";
        }
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        //if you want to create a sub-dir
        root = new File(root, "MotorGimbalConsoleFlights");
        root.mkdir();

        SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
        String date = sdf.format(System.currentTimeMillis());

        // select the name for your file
        fileName = FlightName + "-" + Data.getSeries(nbr).getKey().toString() + date + ".csv";
        root = new File(root, fileName);
        Log.d("Flight win", fileName);
        try {
            Log.d("Flight win", "attempt to write");
            FileOutputStream fout = new FileOutputStream(root);
            fout.write(csv_data.getBytes());
            fout.close();
            Log.d("Flight win", "write done");
            SavedCurves = SavedCurves +
                    fileName +"\n";

        } catch (FileNotFoundException e) {
            e.printStackTrace();

            boolean bool = false;
            try {
                // try to create the file
                bool = root.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (bool) {
                // call the method again
                saveData(nbr, Data);
            } else {
                Log.d("Flight win", "Failed to create flight files");
                //throw new IllegalStateException(getString(R.string.failed_to_create_file_msg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "MotorGimbalConsoleFlights/" + fileName;
    }

    //Share file
    private void shareFile(File file) {

        Uri uri = FileProvider.getUriForFile(
                getContext(),
                getContext().getPackageName() +  ".provider",
                file);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("file/*");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Motor Gimbal has shared with you some info");
        intent.putExtra(Intent.EXTRA_STREAM, uri);


        Intent chooser = Intent.createChooser(intent, "Share File");

        List<ResolveInfo> resInfoList = getContext().getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            this.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }
    /*
    Return the position of the first X value it finds from the beginning
     */
    public int searchX(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getY(i - 1).doubleValue()) && (searchVal <= serie.getY(i).doubleValue())) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    /*
      Return the position of the first X value it finds from the beginning
    */
    public int searchXBack(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getY((nbrData - i) - 1).doubleValue()) && (searchVal <= serie.getY(nbrData - i).doubleValue())) {
                pos = (nbrData - i);
                break;
            }
        }
        return pos;
    }

    /*
    Return the position of the first Y value it finds from the beginning
    */
    public int searchY(XYSeries serie, double searchVal) {
        int nbrData = serie.getItemCount();
        int pos = -1;
        for (int i = 1; i < nbrData; i++) {
            if ((searchVal >= serie.getX(i - 1).doubleValue()) && (searchVal <= serie.getX(i).doubleValue())) {
                pos = i;
                break;
            }
        }
        return pos;
    }
}
