package com.mahali.gpslogger;

/**
 * Created by ktikennedy on 1/17/15.
 */

import java.util.ArrayList;

//import com.publicstaticdroidmain.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GPSSessionBaseAdaptor extends BaseAdapter {
    private static ArrayList<GPSSession> sessionList;

    private LayoutInflater mInflater;

    public GPSSessionBaseAdaptor(Context context, ArrayList<GPSSession> sessionList) {
        // TODO: display meaningful message in ListView when there are no sessions found

        this.sessionList = sessionList;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return sessionList.size();
    }

    public Object getItem(int position) {
        return sessionList.get(position);
    }

    // Necessary?
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.session_row_view, null);
            holder = new ViewHolder();
            holder.txtSessionName = (TextView) convertView.findViewById(R.id.sessionName);
            holder.txtSessionSize = (TextView) convertView.findViewById(R.id.sessionSize);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.txtSessionName.setText(sessionList.get(position).getFileName());

        String s = humanReadableByteCount(sessionList.get(position).getSize(),true);
        holder.txtSessionSize.setText(s);

        return convertView;
    }

    // Taken from here:
    // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    static class ViewHolder {
        TextView txtSessionName;
        TextView txtSessionSize;
    }
}