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

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Created by ktikennedy on 1/17/15.
 */

/*
This class is a wrapper around a GPS data session file, stored in the device's external memory.

Whenever a new instance is created, the constructor set the file name based on the current time. Note that the time is only captured up to second precision, which means that sessions should not be created more rapidly than once per second, otherwise they will be overwritten
 */
public class GPSSession implements Comparable {
    private final String TAG = GPSSession.class.getSimpleName();

    private String absolutePath;
    private String fileName;
    private long size;

    public GPSSession() {
        Log.i(TAG,"New GPSSession created");

        // Get creation time
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));   //make a new calendar, set the time zone to UTC
        Date creationTime = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss"); //formats the date string. last 2 digits of year, 2 digits for month, then day of month, hours, mintues, seconds
        sdf.setCalendar(cal); //have to set the SimpleDateFormat to use this calendar, otherwise it won't output in UTC

        this.fileName = sdf.format(creationTime)+".nvd";  //create filename with current time and extension .nvd. nvd is for novatel. TODO: make this customizable by user

        Log.i(TAG,"file: "+this.fileName);

        size = 0;
    }

    public GPSSession(String fileName,String absolutePath, long size) {
        this.fileName = fileName;
        this.absolutePath = absolutePath;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public long getSize() {return size;}

    @Override
    public int compareTo(Object another) {
        // Cast the object to a GPSSession
        GPSSession theOther = (GPSSession)another;

        //Use the String compareTo method to compare based on date in filename
        return this.fileName.compareTo(theOther.getFileName());
    }
}
