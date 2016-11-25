package com.le2e.le2etruckstop.ui.home.interfaces;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.ArrayList;
import java.util.HashMap;

public interface SearchImpl {
    HashMap<Marker, TruckStop> getMarkerMap();
    void deliverSearchResults(ArrayList<TruckStop> results);
    void turnSearchBlockOff();
}
