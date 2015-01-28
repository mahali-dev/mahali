package com.mahali.gpslogger;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.Toast;


public class ConfigActivity extends ActionBarActivity {

    private final String TAG = ConfigActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String DEFAULT_GPS_CONFIG = "unlogall\r\n"+
            "log,com1,versiona,once\r\n"+
            "ecutoff,10\r\n"+
            "externalclock,disable\r\n"+
            "clockadjust,disable\r\n"+
            "SinBandWidth,0.1,0.0\r\n"+
            "SinTECCalibration,0\r\n"+
            "CPOFFSET,-0.0321,-0.3186,0.0447,0.4605,-0.267,0.1788,-0.1854,-0.1539,0.096,-0.4974,0.2265,0,0.4677,0.1281,-0.2841,-0.0855,-0.2574,0.0255,0,-0.3057,-0.0801,-0.4266,-0.2235,0.1035,0.1833,0.3966,0.0015,-0.0288,0.2868,0.6195,-0.0732,0\r\n"+
            "log,com1,satvisb,ontime,15.0\r\n"+
            "log,com1,waas18b,onchanged\r\n"+
            "log,com1,waas26b,onchanged\r\n"+
            "log,com1,bestposb,ontime,1.0\r\n"+
            "log,com1,rangeb,ontime,1.0\r\n"+
            "log,com1,ismrb,onnew\r\n"+
            "log,com1,gpsephemb,onchanged\r\n"+
            "log,com1,ionutcb,onchanged\r\n";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String curConfig = settings.getString("gpsConfig",DEFAULT_GPS_CONFIG);
        EditText t = (EditText) findViewById(R.id.editTextConfig);
        t.setText(curConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_restoreDefaultConfig) {
            Log.i(TAG, "Restoring default GPS config");
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("gpsConfig",DEFAULT_GPS_CONFIG);
            editor.commit();

            reloadConfig();

            Toast.makeText(ConfigActivity.this, "GPS config restored to app default.", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSaveButtonClicked(View view) {
        // Save GPS config to SharedPreferences?
        Log.i(TAG,"Save button clicked");

        EditText t = (EditText) findViewById(R.id.editTextConfig);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("gpsConfig",t.getText().toString());
        editor.commit();

        Toast.makeText(ConfigActivity.this, "GPS config saved", Toast.LENGTH_LONG).show();

    }

    public void onLoadButtonClicked(View view) {
        // Load from external (e.g. Dropbox)
        Log.i(TAG,"Load button clicked");

    }
    public void onCancelButtonClicked(View view) {
        Log.i(TAG,"Cancel button clicked");

        // Revert to previous config
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String curConfig = settings.getString("gpsConfig",DEFAULT_GPS_CONFIG);

        EditText t = (EditText) findViewById(R.id.editTextConfig);
        t.setText(curConfig);

        Toast.makeText(ConfigActivity.this, "GPS config reverted from saved preferences", Toast.LENGTH_LONG).show();
    }

    public void reloadConfig() {
        // Revert to previous config
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String curConfig = settings.getString("gpsConfig",DEFAULT_GPS_CONFIG);

        EditText t = (EditText) findViewById(R.id.editTextConfig);
        t.setText(curConfig);
    }

}
