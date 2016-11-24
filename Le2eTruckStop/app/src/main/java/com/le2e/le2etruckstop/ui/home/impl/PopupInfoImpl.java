package com.le2e.le2etruckstop.ui.home.impl;


import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;

public interface PopupInfoImpl {
    TruckStop getStopInfoFromMarker(Marker marker);
}
