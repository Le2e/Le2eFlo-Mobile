package com.le2e.le2etruckstop.ui.common;


import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.R;

import java.lang.ref.WeakReference;

public class TruckStopPopupAdapter implements GoogleMap.InfoWindowAdapter {
    private LayoutInflater layoutInflater;

    public TruckStopPopupAdapter(WeakReference<Activity> context){
        layoutInflater = context.get().getLayoutInflater();
    }

    public void setLayoutInflater(WeakReference<Activity> activity){
        layoutInflater = activity.get().getLayoutInflater();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = null;
        if (layoutInflater != null) {
            view = layoutInflater.inflate(R.layout.popup_poi_window, null);
            TextView name = (TextView) view.findViewById(R.id.stop_name);
            TextView line1 = (TextView) view.findViewById(R.id.raw_line_1);

            name.setText(marker.getTitle());
            line1.setText(marker.getSnippet());
        }

        return view;
    }
}
