package com.mahali.gpslogger;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

// Dropbox Includes
import com.dropbox.client2.DropboxAPI;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private UsbManager mUsbManager;

    // The directory, in external storage, where mahali files will be stored
    private final String mahali_directory = "mahali";

    // The directory for the user's public documents directory.
    File dirFile;
    File sessFile;
    BufferedOutputStream bufOS = null;

    //for holding a reference to the file that we're currently reading from/writing to
//    private File currentFile = null;
    GPSSession mCurrentSession = null;

    // List of the previous sessions found by the app
    private ArrayList<GPSSession> sessionList = null;

    //Required dropbox fields. Get app key, app secret, and access type from App Console on dropbox website. Make sure the key matches the key in AndroidManifest.xml!
    private static final String APP_KEY = "2wmhe173wllfuwz";
    private static final String APP_SECRET = "2h6bixl3fsaxx6m";
    private static final Session.AccessType ACCESS_TYPE = Session.AccessType.APP_FOLDER;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: pull a GPS recvr config file from dropbox

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

        sessionList = loadGPSSessions();

        final ListView lv = (ListView) findViewById(R.id.sessionListView);
        lv.setAdapter(new GPSSessionBaseAdaptor(this,sessionList));

        // this method handles the selection of sessions from the list
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = lv.getItemAtPosition(position);
                GPSSession fullObject = (GPSSession)o;
                Log.i(TAG, "You have chosen: " + " " + fullObject.getFileName());

                //TODO: implement a selection menu in front of these options...

                // Try to upload the file to dropbox for the time being
                sendToDB(fullObject.getAbsolutePath());

                // Stub for deleting the file
                deleteFile();
            }
        });

    }

    private void updateSessionListView() {
        sessionList = loadGPSSessions();
        // TODO: display meaningful message in ListView when there are no sessions found

        final ListView lv = (ListView) findViewById(R.id.sessionListView);
        lv.setAdapter(new GPSSessionBaseAdaptor(this,sessionList));
    }

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


        // TODO: Probably want to call lv.setAdapter(...) again, to update the list. See code in onCreate. Will have to create a new GPSSession object
        updateSessionListView();
    }

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
                    // Write data to session file
                    try {
                        bufOS.write(data);
                    } catch (IOException e) {
                        Log.e(TAG, "mListener failed to write data to output buffer" + e.getMessage());
                    }

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
    }

    public ArrayList<GPSSession> loadGPSSessions() {
        Log.i(TAG,"loadGPSSessions");
        File gps_files[] = dirFile.listFiles();

        ArrayList<GPSSession> sessions = new ArrayList<GPSSession>();

        for (int i=0;i<gps_files.length;i++) {
            File f = gps_files[i];
            String fileName = f.getName();

            // First check if file name is long enough to be a mahali gps data file
            if (fileName.length() >= 17) {
                // Grab what should be the '.nvd' file extension, if it's a mahali file
                String test = fileName.substring(13, 17);

                if (test.equals(".nvd")) {
                    // Create new GPS sessions with the file name and length of file in bytes
                    sessions.add(new GPSSession(fileName,f.getAbsolutePath(),f.length()));
                }
            }
        }

        // Reverse order so most recent session is at top of list
        Collections.reverse(sessions);

        return sessions;
    }

    private void deleteFile() {
        // TODO: write code for file deletion. Note that we'll have to call lv.setAdapter(...) again, to update the list
    }

    // Send file to DropBox
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
                response = mDBApi.putFile("/"+fileToSend.getName(), inputStream, fileToSend.length(), null, null);
            } catch (DropboxException e) {
                Log.e(TAG,"DropBox putfile failed!");
            }

            return response;
        }

        // This function is called in the UI (main) thread, after doInBackground returns
        protected void onPostExecute(DropboxAPI.Entry response) {
            if (! (response==null)) {
                Log.i("DbExampleLog", "The uploaded file's rev is: " + response.rev);

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
}
