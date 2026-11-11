package com.motorgimbalconsole.connection;
/**
 *   @description:  This is to select the bluetooth module.
 *
 *   @author: boris.dureau@neuf.fr
 *
 **/

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorgimbalconsole.ConsoleApplication;
import com.motorgimbalconsole.R;
import com.motorgimbalconsole.ShareHandler;
import com.motorgimbalconsole.help.AboutActivity;
import com.motorgimbalconsole.help.HelpActivity;

import java.util.ArrayList;
import java.util.Set;

public class SearchBluetooth extends AppCompatActivity {
    //widgets
    Button btnPaired, btnDismiss;
    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    ConsoleApplication myBT ;

    /**
     * Asks for the "Nearby devices" permissions (Android 12+). Both are requested in one
     * system dialog; the device list is shown as soon as they are granted. The former code
     * requested them and returned, so the user had to press the button again.
     */
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        if (BluetoothConnection.hasConnectPermission(this)) {
                            ensureBluetoothEnabledThenList();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.BT_msg3), Toast.LENGTH_LONG).show();
                        }
                    });

    /**
     * Asks the user to turn Bluetooth on, then lists the paired devices. The former code only
     * did it when the permission was missing, so with Bluetooth off the list stayed empty.
     */
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (myBluetooth != null && myBluetooth.isEnabled()) {
                            pairedDevicesList();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.BT_msg4), Toast.LENGTH_LONG).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Check the local and force it if needed
        myBT = (ConsoleApplication) getApplication();

        setContentView(R.layout.activity_search_bluetooth);

        //Calling widgets
        btnPaired = (Button)findViewById(R.id.button);
        btnDismiss = (Button) findViewById(R.id.butSearchBTDismiss);
        devicelist = (ListView)findViewById(R.id.listView);

        // Bluetooth adapter from the system service (BluetoothAdapter.getDefaultAdapter() is
        // deprecated)
        myBluetooth = BluetoothConnection.getAdapter(this);

        if(myBluetooth == null)
        {
            //Bluetooth Device Not Available
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg1), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                startDeviceSelection();
            }
        });

        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        startDeviceSelection();
    }

    /** Permissions first, then Bluetooth on, then the list of paired devices. */
    private void startDeviceSelection() {
        if (!BluetoothConnection.hasConnectPermission(this)) {
            permissionLauncher.launch(BluetoothConnection.requiredPermissions());
            return;
        }
        ensureBluetoothEnabledThenList();
    }

    /** Lists the paired devices, after asking to turn Bluetooth on if needed. */
    private void ensureBluetoothEnabledThenList() {
        if (!myBluetooth.isEnabled()) {
            enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }
        pairedDevicesList();
    }

    /**
     * Displays the devices already paired in the Android settings (the HC-05 / HC-06 module of
     * the board must be paired first). No discovery is done, so only BLUETOOTH_CONNECT is
     * needed here (BLUETOOTH_SCAN was requested for nothing).
     */
    @SuppressLint("MissingPermission") // checked by hasConnectPermission()
    private void pairedDevicesList()
    {
        if (!BluetoothConnection.hasConnectPermission(this)) {
            return;
        }
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();

        if (pairedDevices != null && pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        }
        else
        {
            //"No Paired Bluetooth Devices Found."
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.BT_msg2), Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            myBT.setAddress(address);
            String moduleName = info.substring(0, info.length() - 17);
            myBT.setModuleName(moduleName);
            finish();
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
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

        //open application settings screen
       /* if (id == R.id.action_settings) {
            Intent i= new Intent(SearchBluetooth.this, AppConfigActivity.class);
            startActivity(i);
            return true;
        }*/
        //share screen
        if (id == R.id.action_share) {
            ShareHandler.takeScreenShot(findViewById(android.R.id.content).getRootView(), this);
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i= new Intent(SearchBluetooth.this, HelpActivity.class);
            i.putExtra("help_file", "help_bluetooth");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i= new Intent(SearchBluetooth.this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
