package com.le2e.le2etruckstop.ui.home_screen;


import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpView;

public interface MapsHomeView extends MvpView {
    void addTruckStopToMap(MarkerOptions options);
}
