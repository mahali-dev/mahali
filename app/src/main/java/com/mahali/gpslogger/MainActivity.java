package com.mahali.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private UsbManager mUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

    }

    public void startSession(View v) {

        // Probe for devices
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        // Get the first port of the first driver
        // TODO: probably shouldn't be hard-coded, but multiple cables are unlikely
        sPort = drivers.get(0).getPorts().get(0);

        // Open a connection
        UsbDeviceConnection connection = mUsbManager.openDevice(sPort.getDriver().getDevice());
        if (connection == null) {
            Log.e(TAG, "Error opening device");
            return;
        }

        try {
            sPort.open(connection);
            // TODO: port configuration should almost certainly be configuration
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            sPort = null;
            return;
        }

        // Start the IO manager thread
        startIoManager();
    }

    public void stopSession(View v) {
        stopIoManager();

        // Block until mSerialIoManager has finished writing and has shutdown?
        //mSerialIoManager.waitForStop();

        // TODO: Close file?
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

    /// Serial Port code
    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    // SerialInputOutputManager.Listen is a subclass of Runnable (this makes it's own thread!)
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }
                @Override
                public void onNewData(final byte[] data) {
                    // TODO: write data to file, or pass to thread that has the file handler?

                    // Loopback the data to the serial port with a non-blocking write
                    //mSerialIoManager.writeAsync(data);

                    // Also push the bytes out to the log
                    String decoded = new String(data);
                    Log.d(TAG, '\n'+decoded+'\n');
                }
            };
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }
    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);

            // TODO: send actual GPS configuration string.
            // NOTE: writeAsync writes into a smallish buffer that we may want to make bigger,
            // otherwise an exception will be thrown.
            String msg = "unlogall\r\nlog,com1,version,ontime,5\r\n"; // causes GPS to spit out its version string every five seconds
            mSerialIoManager.writeAsync(msg.getBytes());
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
