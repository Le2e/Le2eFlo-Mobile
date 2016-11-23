package com.le2e.le2etruckstop.data.model;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.HashMap;

public class TruckStopUtils {
    private HashMap<Marker, TruckStop> stopsMap;

    public TruckStopUtils(){
        stopsMap = new HashMap<>();
    }

    public void clearMarkerMap(){
        stopsMap.clear();
    }

    public void addMarkerToMap(Marker marker, TruckStop data){
        stopsMap.put(marker, data);
    }

    public void removeMarkerFromMap(Marker marker){
        stopsMap.remove(marker);
    }

    public HashMap<Marker, TruckStop> getStopsMap(){
        return stopsMap;
    }

    public TruckStop getTruckStop(Marker marker){
        return stopsMap.get(marker);
    }



    public static String formatTruckStopRawLine(String one, String two, String three){
        StringBuilder sb = new StringBuilder();

        if (one != null)
            sb.append(one);

        if (two != null) {
            sb.append("\n");
            sb.append(two);
        }
        if (three != null) {
            sb.append("\n");
            sb.append(three);
        }

        return sb.toString();
    }
}
