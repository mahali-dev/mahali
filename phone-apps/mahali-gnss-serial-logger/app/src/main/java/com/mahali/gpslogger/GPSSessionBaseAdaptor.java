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

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/*
This is a wrapper class that is necessary for formatting the session list view in the app's main menu
 */
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