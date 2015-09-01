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



import android.widget.TextView;


/*
This class handles a menu to display help text
 */
public class HelpActivity extends ActionBarActivity {

    private final String TAG = HelpActivity.class.getSimpleName();

    // default GPS config for Novatel GPS Station 6 and  GSV4004B receivers.
    public static final String HAAAAALP =
            "The Mahali GNSS Data Logger App\n"+
                    "\n"+
            "Copyright (c) 2015 Massachusetts Institute of Technology\n" +
            "Author(s): Andrew K. Kennedy (kitkennedy8@gmail.com), Ryan W. Kingsbury (ryan.kingsbury@gmail.com)\n" +
            "This software is part of the Mahali Project (PI: V. Pankratius)\n" +
            "http://mahali.mit.edu\n" +
            "\n" +
            "Connect a GPS receiver via USB cable to get started. Off-the-shelf USB to serial converter cables should work fine, but beware finicky ones. " +
                    "Upon connection, a menu should appear allowing you to connect this app with the USB port. " +
                    "Toggle the session control button to start collecting data. Data files are automagically " +
                    "created in this device's external memory, and all known files are indicated in the previous sessions list. " +
                    "Quick tap a session file menu item to upload to Dropbox immediately." +
                    "Press and hold on a session file to share via the Android share API, or delete. " +
                    "Change settings and erase multiple files at once through the 'Settings and Files' menu.";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        Intent intent = getIntent();

        TextView tv_halp = (TextView) findViewById(R.id.help_text);

        tv_halp.setText(HAAAAALP);


        // Set the text view as the activity layout
//        setContentView(textView);

    }

}

