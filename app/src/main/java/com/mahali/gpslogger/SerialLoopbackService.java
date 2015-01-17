package com.mahali.gpslogger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class SerialLoopbackService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_LOOPBACK = "com.example.kingryan.serialtest.action.LOOPBACK";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.kingryan.serialtest.extra.PARAM1";

    private final String TAG = SerialLoopbackService.class.getSimpleName();

    // Log pinger
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 1000;

    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    // TODO: add code here to handle GPS data

                    // Loopback the data to the serial port with a non-blocking write
                    mSerialIoManager.writeAsync(data);

                    // Also push the bytes out to the log
                    String decoded = new String(data);
                    Log.d(TAG, decoded+'\n');
                }
            };


    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionLoop(Context context) {
        Intent intent = new Intent(context, SerialLoopbackService.class);
        intent.setAction(ACTION_LOOPBACK);
        context.startService(intent);
    }

    public SerialLoopbackService() {
        super("SerialLoopbackService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOOPBACK.equals(action)) {
                handleActionLoopback();
            }
        }
    }

    /**
     * Handle action Loopback in the provided background thread with the provided
     * parameters.
     */
    private void handleActionLoopback() {

        // Probe for devices
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        // Get the first port of the first driver
        // TODO: probably shouldn't be hard-coded, but multiple cables are unlikely
        sPort = drivers.get(0).getPorts().get(0);

        // Open a connection
        UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
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

        onDeviceStateChange();

    }


    // NOTE: The three functions below were pulled from the usbSerialForAndroid example project
    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
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
        }
    }


}
