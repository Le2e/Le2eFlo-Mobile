package com.le2e.le2etruckstop.ui.home.interfaces;


public interface MapManagerImpl {
    void getSavedMapType();

    void saveMapTypeToSharedPref(int mapType);

    // search methods
    void deliverSearchResults(int numResults);

    // tracking methods
    void getSavedTrackingState();

    void saveTrackingState(boolean isTracking);

    void toggleTrackingFabIcon(boolean isTracking);

    void getStationsByLoc(String radius, double lat, double lng);
}
