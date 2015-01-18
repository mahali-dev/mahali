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
        holder.txtSessionSize.setText(""+sessionList.get(position).getSize());

        return convertView;
    }

    static class ViewHolder {
        TextView txtSessionName;
        TextView txtSessionSize;
    }
}