package com.example.kingryan.serialtest;

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
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SerialLoopbackService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_LOG_PING = "com.example.kingryan.serialtest.action.LOGPING";
    private static final String ACTION_LOOPBACK = "com.example.kingryan.serialtest.action.LOOPBACK";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.kingryan.serialtest.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.kingryan.serialtest.extra.PARAM2";
    private static final String PORT_PARAM = "ccom.example.kingryan.serialtest.extra.PORTPARAM";

    private final String TAG = SerialLoopbackService.class.getSimpleName();

    // Log pinger
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 1000;

    // LogPing handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    Log.i(TAG, msg.toString());
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

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
                    Log.i(TAG, data.toString());
                }
            };

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionLogPing(Context context, String param1) {
        Intent intent = new Intent(context, SerialLoopbackService.class);
        intent.setAction(ACTION_LOG_PING);
        intent.putExtra(EXTRA_PARAM1, param1);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionLoop(Context context, UsbSerialPort port) {
        Intent intent = new Intent(context, SerialLoopbackService.class);
        intent.setAction(ACTION_LOOPBACK);
        intent.putExtra(PORT_PARAM, port.getPortNumber());
        context.startService(intent);
    }

    public SerialLoopbackService() {
        super("SerialLoopbackService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOG_PING.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                handleActionFoo(param1);
            } else if (ACTION_LOOPBACK.equals(action)) {
                final int portNum = intent.getIntExtra(PORT_PARAM, -1);
                handleActionLoopback(portNum);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1) {
        // Start up the log pinger
        Log.i("handleActionFoo","sending message to Handler"+param1);
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);

    }

    /**
     * Handle action Loopback in the provided background thread with the provided
     * parameters.
     */
    private void handleActionLoopback(int portNum) {

        // Probe for devices
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        // Get the first port of the first driver
        sPort = drivers.get(0).getPorts().get(0);

        // Open a connection
        UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
        if (connection == null) {
            Log.e(TAG, "Error opening device");
            return;
        }


        try {
            sPort.open(connection);
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
