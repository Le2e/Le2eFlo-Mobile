package com.le2e.le2etruckstop.ui.common;


import android.app.Activity;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.home_screen.MapsHomeActivity;

import java.lang.ref.WeakReference;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;

import timber.log.Timber;

public class TruckStopPopupAdapter implements GoogleMap.InfoWindowAdapter {
    private LayoutInflater layoutInflater;
    private HashMap<Marker, TruckStop> stopsMap;
    private MapsHomeActivity activity;
    private HashSet<TruckStop> stationSet;

    public TruckStopPopupAdapter(WeakReference<Activity> activity) {
        this.activity = (MapsHomeActivity) activity.get();
        layoutInflater = activity.get().getLayoutInflater();
        stopsMap = new HashMap<>();
        stationSet = new HashSet<>();
    }

    public void clearMarkerMap() {
        stopsMap.clear();
        stationSet.clear();
    }

    public void addMarkerToMap(Marker marker, TruckStop data) {
        // check if stop has already been added to map - if not, add it
        if (!stationSet.contains(data)) {
            stationSet.add(data);
            stopsMap.put(marker, data);
        }

        /* revisit functionality to handle clearing map when too many unique marks are present
        if(stopsMap.size() > 500){
            Timber.d("hash map getting large, clearing out");
            if(activity != null) {
                activity.clearMarkers();
                stopsMap.clear();
            }
        }
        */
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = null;
        TruckStop data = stopsMap.get(marker);
        Timber.d("num stored markers: --:-- %s", stopsMap.size());
        if (activity != null) {
            if (data != null) {
                if (layoutInflater != null) {
                    view = layoutInflater.inflate(R.layout.popup_poi_window, null);
                    TextView name = (TextView) view.findViewById(R.id.stop_name);
                    TextView street = (TextView) view.findViewById(R.id.stop_street);
                    TextView cityState = (TextView) view.findViewById(R.id.stop_city_state);
                    TextView zip = (TextView) view.findViewById(R.id.stop_zip);
                    TextView dist = (TextView) view.findViewById(R.id.stop_distance);
                    TextView extra = (TextView) view.findViewById(R.id.stop_extra);
                    TextView phone = (TextView) view.findViewById(R.id.stop_phone);

                    setTextViewText(name, data.getName());
                    setTextViewText(street, data.getRawLine1());
                    setTextViewText(zip, data.getZip());

                    setTextViewText(dist, calcDistance(data.getLat(), data.getLng()));
                    setTextViewText(extra, data.getRawLine2());
                    setTextViewText(phone, data.getRawLine3());

                    StringBuilder sb = new StringBuilder();
                    if (data.getCity() != null) {
                        sb.append(data.getCity());
                        sb.append(", ");
                    }

                    if (data.getState() != null)
                        sb.append(data.getState());

                    setTextViewText(cityState, sb.toString());
                }
            }
        }

        return view;
    }

    private void setTextViewText(TextView view, String text) {
        if (text != null)
            view.setText(text);
        else
            view.setVisibility(View.GONE);
    }

    private String calcDistance(String stopLat, String stopLng) {
        LatLng latLng = activity.getCurrentLocation();
        double sLat = Double.parseDouble(stopLat);
        double sLng = Double.parseDouble(stopLng);

        Location sLoc = new Location("");
        sLoc.setLatitude(sLat);
        sLoc.setLongitude(sLng);

        Location cLoc = new Location("");
        cLoc.setLatitude(latLng.latitude);
        cLoc.setLongitude(latLng.longitude);

        double dist = cLoc.distanceTo(sLoc);
        dist = dist * 0.000621371f;
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.UP);

        return df.format(dist) + " miles";
    }
}
