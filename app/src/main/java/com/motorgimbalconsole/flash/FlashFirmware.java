package com.motorgimbalconsole.flash;

/**
 *   @description: This is used to flash the altimeter firmware from the Android device using an OTG cable
 *   so that the store Android application is compatible with altimeter. This works with the
 *   ATMega328 based altimeters as well as the STM32 based altimeters
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.motorgimbalconsole.R;

import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;
import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import java.io.IOException;

import java.io.InputStream;
import java.util.ArrayList;

import static com.physicaloid.misc.Misc.toHexStr;


public class FlashFirmware extends AppCompatActivity {
    Physicaloid mPhysicaloid;

    boolean recorverFirmware = false;
    Boards mSelectedBoard;
    Button btFlash;

    public Spinner spinnerFirmware;
    public ImageView imageAlti;
    TextView tvRead;
    private AlertDialog.Builder builder = null;
    private AlertDialog alert;
    private ArrayList<Boards> mBoardList;
    private UartConfig uartConfig;


    private static final String ASSET_FILE_NAME_MOTORGIMBALE  = "firmwares/2023-02-26_RocketMotorPIDGimbalV1.3.ino.bin";
    private static final String ASSET_FILE_NAME_MOTORGIMBALE_BMP280  = "firmwares/2023-03-25-RocketMotorPIDGimbal_bmp280V1.3.ino.bin";
    private static final String ASSET_FILE_NAME_MOTORGIMBALE_BNO55  = "firmwares/2023-02-26_RocketMotorPIDGimbal_bno055V1.3.ino.bin";
    private static final String ASSET_FILE_NAME_MOTORGIMBALE_BNO55_BMP280  = "firmwares/2023-03-25-RocketMotorPIDGimbal_bno055_bmp280V1.3.ino.bin";


    private static final String ASSET_FILE_RESET_ALTISTM32 = "recover_firmwares/ResetAltiConfigAltimultiSTM32.ino.bin";

    private String[] itemsBaudRate;
    private String[] itemsFirmwares;
    private Spinner dropdownBaudRate;

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_firmware);

        spinnerFirmware = (Spinner) findViewById(R.id.spinnerFirmware);
        itemsFirmwares = new String[]{
                "Gimbal",
                "Gimbal_bno055",
                "Gimbal_BMP280",
                "Gimbal_bno055_BMP280"
        };

        ArrayAdapter<String> adapterFirmware = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsFirmwares);
        spinnerFirmware.setAdapter(adapterFirmware);
        spinnerFirmware.setSelection(0);

        btFlash         = (Button) findViewById(R.id.btFlash);
        tvRead          = (TextView) findViewById(R.id.tvRead);
        imageAlti = (ImageView) findViewById(R.id.imageAlti);

        mPhysicaloid = new Physicaloid(this);
        mBoardList = new ArrayList<Boards>();
        for(Boards board : Boards.values()) {
            if(board.support>0) {
                mBoardList.add(board);
            }
        }

        mSelectedBoard = mBoardList.get(0);
        uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);

        btFlash.setEnabled(true);
        if(mPhysicaloid.open()) {
            mPhysicaloid.setConfig(uartConfig);

        } else {
            //cannot open
            Toast.makeText(this, getResources().getString(R.string.msg13), Toast.LENGTH_LONG).show();
        }


        //baud rate
        dropdownBaudRate = (Spinner)findViewById(R.id.spinnerBaud);
        itemsBaudRate = new String[]{ "300",
                "1200",
                "2400",
                "4800",
                "9600",
                "14400",
                "19200",
                "28800",
                "38400",
                "57600",
                "115200",
                "230400"};
        ArrayAdapter<String> adapterBaudRate = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, itemsBaudRate);
        dropdownBaudRate.setAdapter(adapterBaudRate);
        dropdownBaudRate.setSelection(10);
        spinnerFirmware.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altigimbal, getApplicationContext().getTheme()));
                        imageAlti.setImageResource(R.drawable.altigimbal);
                    }
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altigimbal_bmp280, getApplicationContext().getTheme()));
                    }
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altigimbal_bno055, getApplicationContext().getTheme()));
                    }
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280"))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        imageAlti.setImageDrawable(getResources().getDrawable(R.drawable.altigimbal_bno055_bmp280, getApplicationContext().getTheme()));
                    }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
        builder = new AlertDialog.Builder(this);
        //Running Saving commands
        builder.setMessage(R.string.flash_firmware_long_msg)
                .setTitle(R.string.flash_firmware_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.flash_firmware_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();
        alert.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }
    public void onClickDismiss(View v) {
        close();
        finish();
    }

    public void onClickRecover(View v) {
        String recoverFileName;
        recoverFileName = ASSET_FILE_RESET_ALTISTM32;

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal")) {
            recoverFileName =ASSET_FILE_RESET_ALTISTM32;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280")) {
            recoverFileName =ASSET_FILE_RESET_ALTISTM32;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055")) {
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280")) {
            recoverFileName = ASSET_FILE_RESET_ALTISTM32;
        }

        tvRead.setText("");
        tvRead.setText(getResources().getString(R.string.after_complete_upload));

        if (!itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280")) {
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Recover firmware...
                builder.setMessage(getResources().getString(R.string.msg18))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();
                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(recoverFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        }
        else {
            recorverFirmware = true;
            new UploadSTM32Asyc().execute();
        }
    }
    public void onClickDetect(View v) {
        new DetectAsyc().execute();
    }
    public void onClickFlash(View v) {
        String firmwareFileName;

        firmwareFileName = ASSET_FILE_NAME_MOTORGIMBALE;

        btFlash.setEnabled(true);

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal")) {
            firmwareFileName = ASSET_FILE_NAME_MOTORGIMBALE;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055")) {
            firmwareFileName = ASSET_FILE_NAME_MOTORGIMBALE;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280")) {
            firmwareFileName = ASSET_FILE_NAME_MOTORGIMBALE;
        }

        if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280")) {
            firmwareFileName = ASSET_FILE_NAME_MOTORGIMBALE;
        }
        tvRead.setText("");
        if (!itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280") &&
                !itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280")) {
            try {
                builder = new AlertDialog.Builder(FlashFirmware.this);
                //Flashing firmware...
                builder.setMessage(getResources().getString(R.string.msg10))
                        .setTitle(getResources().getString(R.string.msg11))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                                mPhysicaloid.cancelUpload();
                            }
                        });
                alert = builder.create();
                alert.show();

                mPhysicaloid.setBaudrate(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
                mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(firmwareFileName), mUploadCallback);
            } catch (RuntimeException e) {
                //Log.e(TAG, e.toString());
            } catch (IOException e) {
                //Log.e(TAG, e.toString());
            }
        }
        else{
            recorverFirmware = false;
            new UploadSTM32Asyc().execute();
        }
    }

    public void onClickFirmwareInfo(View v) {
        tvRead.setText("The following firmwares are available:");
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_MOTORGIMBALE);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_MOTORGIMBALE_BNO55);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_MOTORGIMBALE_BMP280);
        tvRead.append("\n");
        tvRead.append(ASSET_FILE_NAME_MOTORGIMBALE_BNO55_BMP280);
    }
    private class DetectAsyc extends AsyncTask<Void, Void, Void>  // UI thread
    {
        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Attempting to detect firmware...
            builder.setMessage(getResources().getString(R.string.detect_firmware))
                    .setTitle(getResources().getString(R.string.msg_detect_firmware))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            String version = "";

            FirmwareInfo firm = new FirmwareInfo(mPhysicaloid);
            firm.open(38400);
            version = firm.getFirmwarVersion();

            tvAppend(tvRead, "Firmware version detected: " + version + "\n");

            if (version.equals("RocketMotorGimbal")) {
                spinnerFirmware.setSelection(0);
            }
            if (version.equals("RocketMotorGimbal_bno055")) {
                spinnerFirmware.setSelection(0);
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }

    private class UploadSTM32Asyc  extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(FlashFirmware.this);
            //Flashing firmware...
            builder.setMessage(getResources().getString(R.string.msg10))
                    .setTitle(getResources().getString(R.string.msg11))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.firmware_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.cancel();
                        }
                    });
            alert = builder.create();
            alert.show();
        }
        @Override
        protected Void doInBackground(Void... voids) {
            if (!recorverFirmware) {
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal"))
                    uploadSTM32(ASSET_FILE_NAME_MOTORGIMBALE, mUploadSTM32Callback);
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055"))
                    uploadSTM32(ASSET_FILE_NAME_MOTORGIMBALE_BNO55, mUploadSTM32Callback);
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_BMP280"))
                    uploadSTM32(ASSET_FILE_NAME_MOTORGIMBALE, mUploadSTM32Callback);
                if (itemsFirmwares[(int) spinnerFirmware.getSelectedItemId()].equals("Gimbal_bno055_BMP280"))
                    uploadSTM32(ASSET_FILE_NAME_MOTORGIMBALE_BNO55, mUploadSTM32Callback);
            } else {
                uploadSTM32(ASSET_FILE_RESET_ALTISTM32, mUploadSTM32Callback);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            alert.dismiss();
        }
    }
    public void uploadSTM32 ( String fileName, UploadSTM32CallBack UpCallback){
        boolean failed =false;
        InputStream is=null;

        try {
            is = getAssets().open(fileName);

        } catch (IOException e) {
            //e.printStackTrace();
            tvAppend(tvRead, "file not found: " + ASSET_FILE_NAME_MOTORGIMBALE+ "\n");
        } catch (Exception e) {
            e.printStackTrace();
            tvAppend(tvRead, "gethexfile : " + ASSET_FILE_NAME_MOTORGIMBALE+ "\n");
        }

        dialogAppend("Starting ...");
        CommandInterface cmd;

        cmd = new CommandInterface(UpCallback, mPhysicaloid);

        cmd.open(Integer.parseInt(itemsBaudRate[(int) this.dropdownBaudRate.getSelectedItemId()]));
        int ret = cmd.initChip();
        if (ret == 1)
            dialogAppend("Chip has been initiated:" + ret);
        else {
            dialogAppend("Chip has not been initiated:" + ret);
            failed = true;
        }
        int bootversion = 0;
        if (!failed) {
            bootversion = cmd.cmdGet();
            //dialogAppend("bootversion:"+ bootversion);
            tvAppend(tvRead, " bootversion:" + bootversion + "\n");
            if (bootversion < 20 || bootversion >= 100) {
                tvAppend(tvRead, " bootversion not good:" + bootversion + "\n");
                failed =true;
            }
        }

        if (!failed) {
            byte chip_id[]; // = new byte [4];
            chip_id = cmd.cmdGetID();
            tvAppend(tvRead, " chip id:" + toHexStr(chip_id, 2) + "\n");
        }

        if (!failed) {
            if (bootversion < 0x30) {
                tvAppend(tvRead,  "Erase 1\n");
                cmd.cmdEraseMemory();
            }
            else {
                tvAppend(tvRead,  "Erase 2\n");
                cmd.cmdExtendedEraseMemory();
            }
        }
        if (!failed) {
            cmd.drain();
            tvAppend(tvRead, "writeMemory" + "\n");
            ret = cmd.writeMemory(0x8000000, is);
            tvAppend(tvRead, "writeMemory finish" + "\n\n\n\n");
            if (ret == 1) {
                tvAppend(tvRead, "writeMemory success" + "\n\n\n\n");
            }
        }
        if (!failed) {
            cmd.cmdGo(0x8000000);
        }
        cmd.releaseChip();
    }


    Physicaloid.UploadCallBack mUploadCallback = new Physicaloid.UploadCallBack() {

        @Override
        public void onUploading(int value) {
            dialogAppend(getResources().getString(R.string.msg12)+value+" %");
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));
        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }
        @Override
        public void onPostUpload(boolean success) {
            if(success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }
            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadErrors err) {
               tvAppend(tvRead, getResources().getString(R.string.msg18)+err.toString()+"\n");
        }

    };

    UploadSTM32CallBack mUploadSTM32Callback = new UploadSTM32CallBack() {

        @Override
        public void onUploading(int value) {

            dialogAppend(getResources().getString(R.string.msg12)+value+" %");
        }

        @Override
        public void onInfo(String value) {
            tvAppend(tvRead, value);
        }

        @Override
        public void onPreUpload() {
            //Upload : Start
            tvAppend(tvRead, getResources().getString(R.string.msg14));

        }

        public void info(String value) {
            tvAppend(tvRead, value);
        }
        @Override
        public void onPostUpload(boolean success) {
            if(success) {
                //Upload : Successful
                tvAppend(tvRead, getResources().getString(R.string.msg16));
            } else {
                //Upload fail
                tvAppend(tvRead, getResources().getString(R.string.msg15));
            }

            alert.dismiss();
        }

        @Override
        //Cancel uploading
        public void onCancel() {
            tvAppend(tvRead, getResources().getString(R.string.msg17));
        }

        @Override
        //Error  :
        public void onError(UploadSTM32Errors err) {
            tvAppend(tvRead, getResources().getString(R.string.msg18)+err.toString()+"\n");
        }

    };
    Handler mHandler = new Handler();
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void dialogAppend(CharSequence text) {
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                alert.setMessage(ftext);
            }
        });
    }

    private void close() {
        if(mPhysicaloid.close()) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_application_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(FlashFirmware.this, HelpActivity.class);
            i.putExtra("help_file", "help_flash_firmware");
            startActivity(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(FlashFirmware.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}