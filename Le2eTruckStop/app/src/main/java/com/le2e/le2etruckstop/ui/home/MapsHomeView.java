package com.le2e.le2etruckstop.ui.home;


import com.le2e.le2etruckstop.ui.base.mvp.core.MvpView;

public interface MapsHomeView extends MvpView {
    void onApiError(Throwable e);
    void printResults(String num);
    void toggleTrackingIcon(boolean isTracking);
    void onMapStateError(Throwable e);
    void onTrackingStateError(Throwable e);
}
