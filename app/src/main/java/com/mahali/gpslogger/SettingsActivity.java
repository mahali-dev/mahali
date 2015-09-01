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

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;


/*
This class handles a menu for various settings for the app
 */
public class SettingsActivity extends ActionBarActivity {

    private final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Intent intent = getIntent();

        String default_db_dir = MainActivity.getDBDirName();

        EditText tv_db_dir = (EditText) findViewById(R.id.db_dir_name);

        tv_db_dir.setText(default_db_dir);


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

    public void setDBDir(View view) {
        EditText tv_db_dir = (EditText) findViewById(R.id.db_dir_name);

        String db_dir = tv_db_dir.getText().toString();

        MainActivity.setDBDirName(db_dir);

        Toast.makeText(this, "Dropbox upload directory modified!", Toast.LENGTH_LONG).show();
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

}

