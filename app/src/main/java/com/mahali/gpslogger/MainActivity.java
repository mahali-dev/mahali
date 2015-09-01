/* The MIT License (MIT)
 * Copyright (c) 2015 Massachusetts Institute of Technology
 *
 * Author(s): Andrew K. Kennedy (kitkennedy8@gmail.com), Ryan W. Kingsbury (ryan.kingsbury@gmail.com)
 * This software is part of the Mahali Project (PI: V. Pankratius)
 * http://mahali.mit.edu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.mahali.gpslogger;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

// Dropbox Includes
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;

// usbSerialForAndroid Includes
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/*
The main screen of the mahali app. Handles start/stop of GPS data collection sessions, listing of stored GPS data sessions, routing to other app menus.
 */
public class MainActivity extends ActionBarActivity {

    // for debug
    private final String TAG = MainActivity.class.getSimpleName();

    private UsbManager mUsbManager;


    // Device local file storage stuff

    // The directory, in device external storage, where GPS data files will be stored
    // TODO: make this directory editable by user? Leave as is for now.
    private final String mahali_directory = "mahali";
    private static String[] gpsFileExtensions = {".nvd",".jps",".bin","MAHALI"};
    private static File dirFile;     // File container for mahali_directory.
    File sessFile;                  // File container for current GPS session. This file is continually augmented with bytes coming from the USB interface as long as session is running.


    // IO related

    BufferedOutputStream bufOS = null;


    // GPS session objects

    // Wraps around file for current GPS data file.
    GPSSession mCurrentSession = null;
    // List of the previous sessions found by the app
    private ArrayList<GPSSession> sessionList = null;


    // Dropbox (DB) stuff

    //Required dropbox fields. Get app key, app secret, and access type from App Console on dropbox website. Make sure the key matches the key in AndroidManifest.xml!
    // TODO: should these be editable?
    private static final String APP_KEY = "iyagryef5rj55xn";//"2wmhe173wllfuwz";
    private static final String APP_SECRET = "3axm6y8j67lovyb";//"2h6bixl3fsaxx6m";
    private static final Session.AccessType ACCESS_TYPE = Session.AccessType.AUTO;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    // the full path of the directory in dropbox to which files will be uploaded for the "quick upload" option (a single tap on a file in the mahali main screen)
    private static String dbDirName = "/Brazil_2015_data/";  //TODO: test this

    // For the Dropbox upload notification
    // Notification code from http://developer.android.com/guide/topics/ui/notifiers/notifications.html
    // Need to create a builder for the notification we're creating
    private final NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.cloud_upload)
                    .setContentTitle("Dropbox upload progress:")
                    .setContentText("0/0");
    // Get the phone's notification manager service
    NotificationManager mNotificationManager = null;
    // mId allows you to update the notification later on.
    final int mId = 1;



    /*
    Called on initial app startup. Sets up all main activity objects, handles resumption of existing dropbox authentication, sets up previous GPS session list, sets up click listener for quick tap DB upload
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: pull a GPS recvr config file from dropbox

        mTimer.start();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);


        // ------------------------------------------------
        //Setup for DropBox connection

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);

        //apparently the below is deprecated, but I got it directly from the tutorial at https://www.dropbox.com/developers/core/start/android
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        // See if we already have information about a dropbox connection stored in the shared preferences file

        // returns the preferences file for "this" (i.e. MainActivity object). See http://developer.android.com/training/basics/data-storage/shared-preferences.html
        // SharedPreferences is a simple way to store basic key-value pairs. In this case we're using it to store dropbox configuration info
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        // TODO: make access token editable?
        // get value of DB OAuth2AccessToken from shared preferences file. If it's not present, will take value "not_created"
        String OAuth2AccessToken = sharedPref.getString("OAuth2AccessToken","not_authenticated");

        Log.i(TAG,"OAuth2AccessToken at startup: "+OAuth2AccessToken);

        // if we do already have info about a DB session stored, set that up
        if (!OAuth2AccessToken.equals("not_authenticated")) {

            Log.i(TAG,"authenticated, at startup: ");
            //sets the OAuth2AccessToken, causing the mDBApi AndroidAuthSession object to be linked (and thus useable for dropbox transactions)
            mDBApi.getSession().setOAuth2AccessToken(OAuth2AccessToken);
            assert mDBApi.getSession().isLinked();
        }

        // ------------------------------------------------


        // TODO: make sure external storage is mounted/available see info here:
        // http://developer.android.com/training/basics/data-storage/files.html#WriteExternalStorage
        dirFile = new File(Environment.getExternalStorageDirectory(),mahali_directory);
        if (!dirFile.mkdirs()) {
            Log.i(TAG, "Directory not created - it already exists!");
        }

        Log.i(TAG, "Directory loaded: "+dirFile.exists());
        final ListView lv = (ListView) findViewById(R.id.sessionListView);

        updateSessionListView();


        // this method handles the selection of sessions from the list (single quick tap on session)
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = lv.getItemAtPosition(position);
                GPSSession fullObject = (GPSSession) o;
                Log.i(TAG, "You have chosen: " + " " + fullObject.getFileName());

                // Try to upload the file to dropbox
                sendToDB(fullObject.getAbsolutePath());
            }
        });

        // For Dropbox upload notification. Can only get system service once onCreate has been called.
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.sessionListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

            menu.setHeaderTitle(sessionList.get(info.position).getFileName());
            String[] menuItems = getResources().getStringArray(R.array.sessionLongMenu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    /*
    The below handles a long press by the user on an individual session. It allows the user to share a session file via any of the available share options on the device (via the standard share api)
    Also, the user can delete a session file from the device's external memory.
    */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = getResources().getStringArray(R.array.sessionLongMenu);
        String menuItemName = menuItems[menuItemIndex];
        String listItemName = sessionList.get(info.position).getFileName();

        GPSSession clickedSession = sessionList.get(info.position);
        Log.v(TAG,String.format("Selected %s for item %s", menuItemName, listItemName));

        // Share session
        if (menuItemIndex==0) {
            Log.v(TAG,"Sharing session "+clickedSession.getFileName());
            shareSession(clickedSession);
        }

        // Delete session
        if (menuItemIndex==1) {
            Log.v(TAG,"Deleting session "+clickedSession.getFileName());
            deleteSession(clickedSession);
        }

        return true;
    }

    /*
    Updates list of GPS sessions
     */
    private void updateSessionListView() {
        final ListView lv = (ListView) findViewById(R.id.sessionListView);

        sessionList = loadGPSSessions();

        lv.setAdapter(new GPSSessionBaseAdaptor(this,sessionList));
        registerForContextMenu(lv);
    }

    /*
    Handles start of a GPS session, setting up USB io manager
     */
    public void startSession(View v) throws IOException {
        // Throws IOException when something goes wrong

        // Probe for devices
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        // Get the first port of the first driver
        // TODO: probably shouldn't be hard-coded, but multiple cables are unlikely
        try {
            sPort = drivers.get(0).getPorts().get(0);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG,"Serial port not available");
            throw new IOException("Serial port not available");
        }

        // Open a connection
        UsbDeviceConnection connection = mUsbManager.openDevice(sPort.getDriver().getDevice());
        if (connection == null) {
            Log.e(TAG, "Error opening USB device");
            throw new IOException("Error opening USB device");
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
            throw new IOException("Error configuring USB device:" + e.getMessage());
        }

        Log.i(TAG,"startSession: creating new GPS session");
        mCurrentSession = new GPSSession();
        sessFile = new File(dirFile.getPath(),mCurrentSession.getFileName());
        if (sessFile.exists()) {
            Log.e(TAG,"Session file already exists!");
            throw new IOException("Session file already exists: " + mCurrentSession.getFileName());
        }
        // Create file
        try {
            boolean fileCreated = sessFile.createNewFile();
            Log.i(TAG, "fileCreated: " + fileCreated);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create new file: " + e.getMessage());
            throw new IOException("Failed to create new file: " + e.getMessage());
        }
        // Create output buffer
        try {
            bufOS = new BufferedOutputStream(new FileOutputStream(sessFile));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found exception: " + e.getMessage());
            throw new IOException("File not found exception during buffer creation");
        }

        // Start the IO manager thread
        startIoManager();
    }

    /*
    Handles takedown of a GPS session, adds to session list
     */
    public void stopSession(View v) {
        stopIoManager();

        // Block until mSerialIoManager has finished writing and has shutdown?
        //mSerialIoManager.waitForStop();

        try {
            bufOS.flush();
            bufOS.close();
        } catch (IOException e) {
            Log.e(TAG, "stopSession failed to flush or close file" + e.getMessage());
        }

        updateSessionListView();

        mCurrentSession = null;

    }

    /*
    Opens GPS config menu
     */
    public void onConfigButtonClicked(View view) {

        Intent intent = new Intent(this, ConfigActivity.class);
        startActivity(intent);

    }

    /*
    Hanldes user clicks of session toggle button
     */
    public void onSessionToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();

        if (on) {
            // Handle toggle on
            Log.i(TAG,"Starting session.");
            try {
                startSession(view);
            } catch (IOException e) {
                // Failed to start session
                Toast.makeText(this, e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                ((ToggleButton) view).setChecked(false);
            }

        } else {
            // Handle toggle off
            Log.i(TAG, "Stopping session.");
            stopSession(view);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /*
    Serial Port code
     */

    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    // TODO: sychronize the variable below...it's being shared between threads
    private static Integer serialStatsRxBytes = 0;

    // Provides periodic UI updates with bytes RXed stats
    Thread mTimer = new Thread() {
        public void run () {
            for (;;) {
                // do stuff in a separate thread

                mHandler.sendEmptyMessage(serialStatsRxBytes);

                //uiCallback.sendEmptyMessage(0);
                try {
                    Thread.sleep(500);    // sleep for 3 seconds
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    };

    // Update UI with status message
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            final TextView tv = (TextView) findViewById(R.id.textViewStatus);

            if (mCurrentSession!=null) {
                final Integer rxBytes = (Integer) msg.what;
                final String s = NumberFormat.getIntegerInstance().format(rxBytes);
                tv.setText("Name: "+mCurrentSession.getFileName()+"\nBytes received: "+s);
            } else {
                tv.setText("Data capture is inactive");
            }
        }
    };

    // SerialInputOutputManager.Listen is a subclass of Runnable (this makes it's own thread!)
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }
                @Override
                public void onNewData(final byte[] data) {
                    // Write data to session file
                    try {
                        bufOS.write(data);
                    } catch (IOException e) {
                        Log.e(TAG, "mListener failed to write data to output buffer" + e.getMessage());
                    }

                    serialStatsRxBytes += data.length;

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
        serialStatsRxBytes = 0;

        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);

            SharedPreferences settings = getSharedPreferences(ConfigActivity.PREFS_NAME, 0);
            String curConfig = "\r\n"+
                    settings.getString("gpsConfig", ConfigActivity.DEFAULT_GPS_CONFIG)+
                    "\r\n";
            Log.i(TAG,"Sending to GPS"+curConfig);

            // NOTE: writeAsync writes into a smallish buffer that we may want to make bigger,
            // otherwise an exception will be thrown.
            // TODO: do we need to add CR+LF chars here for the Novatel?
            mSerialIoManager.writeAsync(curConfig.getBytes());
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
        //        return true;
    }

    /*
    Handles selection of options menu from main menu
     */
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
        else if (id == R.id.action_help) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            startActivity(helpIntent);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /*
    Called whenever we return to main menu. Handles DB authentication, because a first time DB authentication will switch out of the app, get an access token, and return to the app, causing onResume to be called
    Also update session list view
     */
    protected void onResume() {
        super.onResume();

        Log.i(TAG,"calling onResume");

        // The only way this if block should be able to execute is if we just returned from authorizing the app for dropbox access
        if ( !mDBApi.getSession().isLinked() && mDBApi.getSession().authenticationSuccessful()) {
            Log.i(TAG,"onResume, inside authenticationSuccessful code");

            String accessToken = null;
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }

            Log.i(TAG,"onResume, after startOAuth2Authentication");
            //Assuming onResume has been called at this point


            Log.i(TAG,"accessToken: "+accessToken);
            //need to store accessToken in shared preferences

            if (!(accessToken==null)) {
                SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("OAuth2AccessToken", accessToken);
                editor.commit();

                Log.i(TAG,"stored as: "+sharedPref.getString("OAuth2AccessToken","not_authenticated"));

                Toast.makeText(MainActivity.this,"Now linked with DropBox. OAuth2 access token stored for future use", Toast.LENGTH_LONG).show();
            }

        }

        updateSessionListView(); // update the session list in case we returned from the settings activity
    }

    /*
    Loads GPS sessions from files present in device external storage
     */
    public ArrayList<GPSSession> loadGPSSessions() {
        Log.i(TAG,"loadGPSSessions");
        File gps_files[] = dirFile.listFiles();

        ArrayList<GPSSession> sessions = new ArrayList<GPSSession>();

        for (int i=0;i<gps_files.length;i++) {
            File f = gps_files[i];
            String fileName = f.getName();

            if (verifyFileType(fileName)) {
                sessions.add(new GPSSession(fileName,f.getAbsolutePath(),f.length()));
            }
        }

        // Reverse order so most recent session is at top of list
        Comparator<GPSSession> c = Collections.reverseOrder();
        Collections.sort(sessions,c);

        return sessions;
    }

    // Verify that the file extension of fileName is one of those in gpsFileExtensions list
    public static boolean verifyFileType(String fileName) {

        int len  = fileName.length();

        for (int i=0;i<gpsFileExtensions.length;i++) {
            String fileExt = gpsFileExtensions[i];

            if (fileName.indexOf(fileExt) != -1) {
                return true;
            }
        }

        return false;
    }

    /*
    Handles sharing a GPS session file through standard android share api
     */
    private void shareSession(GPSSession sess) {
        File mSessFile = new File(dirFile.getPath(),sess.getFileName());

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mSessFile));
        sendIntent.setType("application/octet-stream"); // This seems to be a good MIME type for raw binary data
        startActivity(sendIntent);
    }

    /*
    Handles session deletion from external memory
     */
    private void deleteSession(GPSSession sess) {
        File mSessFile = new File(dirFile.getPath(),sess.getFileName());
        Log.v(TAG,"deteleting "+mSessFile.getAbsolutePath());
        boolean deleted = mSessFile.delete();
        updateSessionListView();
    }

    /*
    Send file to DropBox. This method is called when the user performs a quick tap on an individual session, and allows for quick upload to user specified dropbox directory.
     */
    public void sendToDB(String absolutePath) {
        Log.i(TAG, "calling sendToDB");

        File fileToSend = new File(absolutePath);

        if ( fileToSend==null ) {
            Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_LONG).show();
            return;
        }

        if ( ! (mDBApi.getSession().isLinked())) {
            connectToDB();
            if ( ! (mDBApi.getSession().isLinked())) {
                return;
            }
        }

        if (fileToSend.exists()) {
            Toast.makeText(MainActivity.this,"Attempting to send to DropBox", Toast.LENGTH_LONG).show();

            // Have to call an asynchronous task for this
            new DBSendFileTask().execute(fileToSend);
        } else {
            Toast.makeText(MainActivity.this,"No content in file to send", Toast.LENGTH_LONG).show();
        }

    }

    /*
    Asynchronous task that handles actual sending of file to dropbox.
     */
    private class DBSendFileTask extends AsyncTask<File, Void, DropboxAPI.Entry> {

        // This function is called in the background thread
        protected DropboxAPI.Entry doInBackground(File... files) {

            FileInputStream inputStream = null;

            File fileToSend = files[0];

            try {
                inputStream = new FileInputStream(fileToSend);
            } catch (FileNotFoundException e) {
                Log.e(TAG,e.getMessage());
            }

            Log.i(TAG,"length of file: "+fileToSend.length());

            DropboxAPI.Entry response = null;
            try {

                // Send the notification
                mNotificationManager.notify(mId, mBuilder.build());

                ProgressListener listener = new ProgressListener() {
                    @Override
                    public void onProgress(long l, long l2) {
                        Log.i(TAG,"Dropbox progress made!");
                        mBuilder.setContentText(l+"/"+l2);
                        mNotificationManager.notify(mId, mBuilder.build());
                    }
                };


                response = mDBApi.putFile(dbDirName+fileToSend.getName(), inputStream, fileToSend.length(), null, listener);
            } catch (DropboxException e) {
                Log.e(TAG,"DropBox putfile failed!");
            }

            return response;
        }

        // This function is called in the UI (main) thread, after doInBackground returns
        protected void onPostExecute(DropboxAPI.Entry response) {
            if (! (response==null)) {
                Log.i("DbExampleLog", "The uploaded file's rev is: " + response.rev);

                mBuilder.setContentText("Success!");
                mNotificationManager.notify(mId, mBuilder.build());
                Toast.makeText(MainActivity.this,"File uploaded to DropBox. Path: "+response.path, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Starts Dropbox authentication if has not occurred yet
    public void connectToDB() {
        Log.i(TAG, "calling connectToDB");

        if (!mDBApi.getSession().isLinked()) {
            //get user to authorize app. Will prompt the user to login to dropbox externally, either through browser or dropbox app
            mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
        }
        else {
//            Toast.makeText(MainActivity.this,"Already linked with DropBox!", Toast.LENGTH_LONG).show();
        }

    }


    /*
    Helper methods...
     */

    public static String getDBDirName() {
        return dbDirName;
    }

    public static void setDBDirName(String dbDirName) {
        MainActivity.dbDirName = dbDirName;
    }

    public static File getDirFile() {
        return dirFile;
    }

    public static void setDirFile(File dirFile) {
        MainActivity.dirFile = dirFile;
    }

}
