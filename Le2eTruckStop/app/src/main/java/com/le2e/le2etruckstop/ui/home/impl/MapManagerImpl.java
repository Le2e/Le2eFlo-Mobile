package com.le2e.le2etruckstop.ui.home.impl;


public interface MapManagerImpl {
    void getSavedMapType();
    void turnTrackingOnByDelay(int delay);
    void delayedStationRequest(int delay, String radius, double lat, double lng, boolean saveOldMarkers);
    void saveMapTypeToSharedPref(int mapType);
}
