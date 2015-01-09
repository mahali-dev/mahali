package com.example.kingryan.serialtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
    private static final String ACTION_BAZ = "com.example.kingryan.serialtest.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.kingryan.serialtest.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.kingryan.serialtest.extra.PARAM2";

    // Log pinger
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 1000;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    Log.i("IntentServicePinger", msg.toString());
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
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
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SerialLoopbackService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
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
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
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
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
