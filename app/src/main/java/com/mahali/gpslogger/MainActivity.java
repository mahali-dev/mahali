package com.mahali.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private UsbManager mUsbManager;

    // The directory, in external storage, where mahali files will be stored
    private final String mahali_directory = "new_files";

    // The directory for the user's public documents directory.
    File dirFile;

    //for holding a reference to the file that we're currently reading from/writing to
//    private File currentFile = null;

    // List of the previous sessions found by the app
//    private ArrayList<GPSSession> sessionList = new ArrayList<GPSSession>();
    private ArrayList<GPSSession> sessionList = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        dirFile = new File(Environment.getExternalStorageDirectory(),mahali_directory);
        if (!dirFile.mkdirs()) {
            Log.i(TAG, "Directory not created - it already exists!");
        }

        Log.i(TAG, "Directory loaded: "+dirFile.exists());

        sessionList = loadGPSSessions();

//        Just creating a temporary list of gps sessions for now
//        sessionList.add(new GPSSession());
//        try {Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        sessionList.add(new GPSSession());
//        try {Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        sessionList.add(new GPSSession());


        final ListView lv = (ListView) findViewById(R.id.sessionListView);
        lv.setAdapter(new GPSSessionBaseAdaptor(this,sessionList));

        // this method handles the selection of sessions from the list
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = lv.getItemAtPosition(position);
                GPSSession fullObject = (GPSSession)o;
                Log.i(TAG, "You have chosen: " + " " + fullObject.getFileName());

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

    public ArrayList<GPSSession> loadGPSSessions() {
        Log.i(TAG,"loadGPSSessions");
        File gps_files[] = dirFile.listFiles();

        ArrayList<GPSSession> sessions = new ArrayList<GPSSession>();

        for (int i=0;i<gps_files.length;i++) {
            String fileName = gps_files[i].getName();
            if (fileName.length() >= 17) {
                String test = fileName.substring(13, 17);

                if (test.equals(".nvd")) {
                    sessions.add(new GPSSession(fileName, gps_files[i].length()));
                }
            }
        }

        return sessions;
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
