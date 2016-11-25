package com.le2e.le2etruckstop.ui.home;


import com.le2e.le2etruckstop.ui.base.mvp.core.MvpView;

interface MapsHomeView extends MvpView {
    void onError(Throwable e);
    void printResults(String num);
    void toggleTrackingIcon(boolean isTracking);
}
