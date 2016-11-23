package com.le2e.le2etruckstop.ui.home_screen;


import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpView;

public interface MapsHomeView extends MvpView {
    void addTruckStopToMap(MarkerOptions options, TruckStop details);
    void clearMarkers();
    void turnTrackingOn();
    void onError(Throwable e);
}
