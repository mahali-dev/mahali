package com.mahali.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private UsbManager mUsbManager;
    private ArrayList<GPSSession> sessionList = new ArrayList<GPSSession>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        String threeStrings[] = {"hello","kitty","!"};

//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, threeStrings);

        sessionList.add(new GPSSession());
        try {Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sessionList.add(new GPSSession());
        try {Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sessionList.add(new GPSSession());
        try {Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sessionList.add(new GPSSession());
        try {Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sessionList.add(new GPSSession());
        final ListView lv = (ListView) findViewById(R.id.sessionListView);
        lv.setAdapter(new GPSSessionBaseAdaptor(this,sessionList));

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = lv.getItemAtPosition(position);
                GPSSession fullObject = (GPSSession)o;
                Toast.makeText(MainActivity.this, "You have chosen: " + " " + fullObject.getFileName(), Toast.LENGTH_LONG).show();
            }
        });

    }

    public void startSession(View v) {

        UsbSerialPort mUsbSerialPort;

        // Handle button click
        Log.i(TAG,"Probing for USB devices, then starting background service.");

        // TODO: the probe process should probably be handled in an AsyncTask...see example project
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        Log.i(TAG,"Number of drivers: "+Integer.toString(drivers.size()));

        if (drivers.size()>0) {
            // Get the first port of the first driver
            mUsbSerialPort = drivers.get(0).getPorts().get(0);
            Log.i(TAG, "Found device: " + mUsbSerialPort.getDriver().toString());

            // Start the loopback service
            Log.i(TAG, "Starting loopback service");
            SerialLoopbackService.startActionLoop(this);
        } else {
            Log.i(TAG,"No devices/ports found.  Can't start loopback service.");
        }

    }

    public void stopSession(View v) {

        // TODO: Code to stop session

    }

    public void onSessionToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();

        if (on) {
            // Handle toggle on
            Log.i(TAG,"Starting session.");

            startSession(view);

        } else {
            // Handle toggle off
            Log.i(TAG, "Stopping session.");

            stopSession(view);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
