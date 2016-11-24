package com.le2e.le2etruckstop.data.manager;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.HashMap;
import java.util.HashSet;

public class StationMapManager {
    private HashMap<Marker, TruckStop> markersMap;
    private HashSet<TruckStop> stationSet;

    public StationMapManager() {
        markersMap = new HashMap<>();
        stationSet = new HashSet<>();
    }

    // adds single marker and associated truck details to set data
    public void addMarkerToMapManager(Marker marker, TruckStop data) {
        // check if stop has already been added to map - if not, add it
        if (!stationSet.contains(data)) {
            stationSet.add(data);
            markersMap.put(marker, data);
        }

        /* revisit functionality to handle clearing map when too many unique marks are present
        if(markersMap.size() > 500){
            Timber.d("hash map getting large, clearing out");
            if(activity != null) {
                activity.clearMarkers();
                markersMap.clear();
            }
        }
        */
    }

    public TruckStop getStopInfoFromMarker(Marker marker) {
        return markersMap.get(marker);
    }

    public HashMap<Marker, TruckStop> getMarkersMap(){
        return markersMap;
    }

    // clears out set data for marker/truck stop details
    public void clearMapMarkers() {
        markersMap.clear();
        stationSet.clear();
    }
}
