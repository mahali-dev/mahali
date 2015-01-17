package com.mahali.gpslogger;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Created by ktikennedy on 1/17/15.
 */
public class GPSSession {
    private final String TAG = GPSSession.class.getSimpleName();

    private String fileName;
    private Date creationTime;  // Can we actually store a date object to file?
    private int size;

    public GPSSession() {
        Log.i(TAG,"New GPSSession created");

        // Get creation time
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));   //make a new calendar, set the time zone to UTC
        this.creationTime = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss"); //formats the date string. last 2 digits of year, 2 digits for month, then day of month, hours, mintues, seconds
        sdf.setCalendar(cal); //have to set the SimpleDateFormat to use this calendar, otherwise it won't output in UTC

        this.fileName = sdf.format(creationTime)+".nvd";

        Log.i(TAG,"file: "+this.fileName);

        size = 0;
    }

    public GPSSession(String fileName) {
        Log.i(TAG,"New GPSSession created with file name");

        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getReadableDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss"); //formats the date string. More readable here

        Log.i(TAG,"heyhey");

        return sdf.format(creationTime)+" "+sdf.getTimeZone().getDisplayName(); //return the time, followed by the time zone
//        return "blah";
    }

    public int getSize() {return size;}

}
