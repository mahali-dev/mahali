package com.mahali.gpslogger;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * Created by ktikennedy on 1/17/15.
 */
public class GPSSession {
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

        this.fileName = sdf.format(creationTime)+".nvd";

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

}
