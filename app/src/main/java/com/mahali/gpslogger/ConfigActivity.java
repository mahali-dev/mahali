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


public class ConfigActivity extends ActionBarActivity {

    private final String TAG = ConfigActivity.class.getSimpleName();

    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String DEFAULT_GPS_CONFIG = "This is the default config.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

/*    @Override
    protected void onResume() {
        // Restore preferences
        // TODO: this is probably not where I should be doing this
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String curConfig = settings.getString("gpsConfig",DEFAULT_GPS_CONFIG);
        Log.i(TAG,"read config from shared prefs:\n"+curConfig+"\n");
        EditText t = (EditText) findViewById(R.id.editTextConfig);
        t.setText(curConfig);
    }*/


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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_config, container, false);
            return rootView;
        }
    }

    public void onSaveButtonClicked(View view) {
        // Save GPS config to SharedPreferences?
        Log.i(TAG,"Save button clicked");

        EditText t = (EditText) findViewById(R.id.editTextConfig);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("gpsConfig",t.getText().toString());
        editor.commit();
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

    }

}
