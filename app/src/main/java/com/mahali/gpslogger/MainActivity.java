package com.mahali.gpslogger;

import android.content.Context;
import android.content.Intent;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
