/**
 * Created by ktikennedy on 4/12/15.
 */


package com.mahali.gpslogger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// for HTTP requests over wifi connection to Edison
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

// For json object parsing
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


public class SettingsActivity extends ActionBarActivity {

    private final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();
        String default_IP = MainActivity.getServerIP();
        String default_port = MainActivity.getServerPort();

        EditText tv_server_ip = (EditText) findViewById(R.id.server_ip);
        EditText tv_server_port = (EditText) findViewById(R.id.server_port);

        tv_server_ip.setText(default_IP);
        tv_server_port.setText(default_port);

        // Set the text view as the activity layout
//        setContentView(textView);

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

    public void setIP(View view) {
        EditText tv_server_ip = (EditText) findViewById(R.id.server_ip);

        String ip = tv_server_ip.getText().toString();

        MainActivity.setServerIP(ip);

        Toast.makeText(this, "Server IP set!", Toast.LENGTH_LONG).show();
    }

    public void setPort(View view) {
        EditText tv_server_port = (EditText) findViewById(R.id.server_port);

        String port = tv_server_port.getText().toString();

        MainActivity.setServerPort(port);

        Toast.makeText(this, "Server port set!", Toast.LENGTH_LONG).show();
    }

    public void downloadFiles(View view) {
        Toast.makeText(SettingsActivity.this,"Attempting file download from server", Toast.LENGTH_LONG).show();

        // Have to call an asynchronous task for this
        new SendHttpRequestTask().execute("blah");

    }

    public void deleteFiles(View view) {
        EditText tv_del_files = (EditText) findViewById(R.id.delete_string);

        String delete_string = tv_del_files.getText().toString();

        if (delete_string.equals("")) {
            Toast.makeText(this, "No delete string specified", Toast.LENGTH_LONG).show();
            return;
        }

        File localFiles[] = MainActivity.getDirFile().listFiles();

        Log.i(TAG, "delete_string "+delete_string);

        for (int i=0;i<localFiles.length;i++) {
            File localFile = localFiles[i];
            String fileName = localFile.getName();

            if (fileName.indexOf(delete_string) != -1 ) {
                Log.i(TAG, "file to delete: "+fileName);
                localFile.delete();
            }
        }

        Toast.makeText(this, "Files with substring "+delete_string+" deleted", Toast.LENGTH_LONG).show();

    }

    /* This tasks figures out which files that are present on the server are NOT present on this phone, and downloads them.
     Note that it was decided to figure out LOCALLY which files to download, because we don't really want the server deciding that for us.
       The update process could be done with fewer HTTP transactions the other way, but it gives up control that the end user should have
      */
    private class SendHttpRequestTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            int numberOfFilesDownloaded = -1;

            Log.i(TAG, "calling SendHttpRequestTask");

            // TODO: implement a timeout on the HTTP request. Currently It'll just keep spinning its wheels

            try {

                // First get list of new files on server

                HttpClient client = new DefaultHttpClient();
                String getURL = "http://"+MainActivity.getServerIP()+":"+MainActivity.getServerPort()+"/logs/return_files_json/science" ;
                HttpGet get = new HttpGet(getURL);
                HttpResponse responseGet = client.execute(get);
                StatusLine statusLine = responseGet.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                Log.i(TAG, "first statuscode: " + statusCode);

                String response_string = "";
                if (statusCode == 200 ) {
                    HttpEntity resEntityGet = responseGet.getEntity();

                    //do something with the response
                    response_string = EntityUtils.toString(resEntityGet);
//                    Log.i(TAG, response_string);
                }
                else {
                    numberOfFilesDownloaded = -1;
                    return new Integer(numberOfFilesDownloaded);
                }

                // Then figure out what new files we need to download

                JSONParser jsonParser = new JSONParser();
                // TODO: unhack this. Depending on "result", it seems that the JSON object created can be of type JSONObject or JSONArray
//                JSONObject jsonObject = (JSONObject) jsonParser.parse(result);
                JSONArray jsonFilesArray= (JSONArray) jsonParser.parse(response_string);
                Log.i(TAG, "remote toString: " + jsonFilesArray.toString());

                JSONArray filesToDownload = compareRemoteFiles(jsonFilesArray);
                Log.i(TAG, "files to download toString: " + filesToDownload.toString());

                // Then download those files

                if (filesToDownload.size() > 0) {

                    for (int i=0;i<filesToDownload.size();i++) {
                        String fileName = (String) filesToDownload.get(i);

                        getURL = "http://"+MainActivity.getServerIP()+":"+MainActivity.getServerPort()+"/logs/science/"+ fileName;
                        get = new HttpGet(getURL);
                        responseGet = client.execute(get);
                        statusLine = responseGet.getStatusLine();
                        statusCode = statusLine.getStatusCode();

                        StringBuilder builder = new StringBuilder();

                        Log.i(TAG, "second statuscode: " + statusCode);

                        if (statusCode == 200 ) { //signifies success!

                            HttpEntity resEntityGet = responseGet.getEntity();

                            InputStream content = resEntityGet.getContent();

                            if (content != null) {
                                File newFile = new File(MainActivity.getDirFile().getPath(), fileName);
                                FileOutputStream fos = new FileOutputStream(newFile);
                                byte buff[] = new byte[128];

                                while (true) {
                                    int readbyte = content.read(buff);

                                    if (readbyte <= 0)
                                        break;

                                    Log.i(TAG, "String bytes read: " + new String(buff, "UTF-8"));
                                    fos.write(buff, 0, readbyte);
                                }

                                content.close();

                                // for every 5 files we download, publish progress
                                if ( (i+1)%5 == 0) {
                                    publishProgress(i + 1, filesToDownload.size());
                                }
                            }

                        }
                        else {
                            numberOfFilesDownloaded -= 10;
                        }
                    }

                    numberOfFilesDownloaded = filesToDownload.size(); //if we get here, we have successfully downloaded all the files

                }
                else {
                    numberOfFilesDownloaded = 0;
                }

            } catch (ParseException ex) {
                ex.printStackTrace();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return numberOfFilesDownloaded;
        }

        protected void onProgressUpdate(Integer numberOfFilesDownloaded,Integer numberOfFilesToDownload) {
            Toast.makeText(SettingsActivity.this, numberOfFilesDownloaded+" of "+numberOfFilesToDownload+" downloaded", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Integer numberOfFilesDownloaded) {

            if (numberOfFilesDownloaded > 0) {

                Toast.makeText(SettingsActivity.this, "Files downloaded! Number of files: "+numberOfFilesDownloaded, Toast.LENGTH_LONG).show();
            }
            else if (numberOfFilesDownloaded == 0) {
                Toast.makeText(SettingsActivity.this, "No new files to download", Toast.LENGTH_LONG).show();
            }
            else if (numberOfFilesDownloaded == -1) {
                Toast.makeText(SettingsActivity.this, "No files downloaded. No connection to server", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(SettingsActivity.this, "No files downloaded. Error code: "+numberOfFilesDownloaded, Toast.LENGTH_LONG).show();
            }

        }

        // Figure out which files we want to download from the server
        // TODO: optimize this function. I'm sure there's a better way to do this than blindly stepping through all the files in both arrays
        private JSONArray compareRemoteFiles(JSONArray remoteFiles) {
            Log.i(TAG, "calling compareRemoteFiles");

            JSONArray filesToDownload = new JSONArray();

            File localFiles[] = MainActivity.getDirFile().listFiles();

//            Log.i(TAG, "num remote files: "+remoteFiles.size());

            for (int i=0;i<remoteFiles.size();i++) {
                String remoteFileName = (String) remoteFiles.get(i);

                if (!MainActivity.verifyFileType(remoteFileName)) {
                    continue;  // skip this file if it's not of our desired gps file types
                }

                Boolean foundLocally = false;

                int j = 0;
                while (!foundLocally && j<localFiles.length) {
                    File f = localFiles[j];

                    if (remoteFileName.equals(f.getName())) {
                        foundLocally = true;
                    }

                    j++;

                }

                if (!foundLocally) {
                    filesToDownload.add(remoteFileName);
                }

            }

            return filesToDownload;

        }
    }
}

