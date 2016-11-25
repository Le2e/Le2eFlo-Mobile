package com.le2e.le2etruckstop.ui.home.interfaces;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

public interface PopupInfoImpl {
    TruckStop getStopInfoFromMarker(Marker marker);
    LatLng getCurrentLocation();
}
