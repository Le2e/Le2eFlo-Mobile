package com.le2e.le2etruckstop.ui.home.impl;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.ArrayList;
import java.util.HashMap;

public interface SearchImpl {
    HashMap<Marker, TruckStop> getMarkerMap();
    void deliverSearchResults(ArrayList<TruckStop> results);
}
