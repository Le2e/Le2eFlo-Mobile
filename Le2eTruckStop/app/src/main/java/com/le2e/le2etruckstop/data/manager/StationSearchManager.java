package com.le2e.le2etruckstop.data.manager;


import android.os.Handler;

import com.google.android.gms.maps.model.Marker;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.home.impl.SearchImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import timber.log.Timber;

public class StationSearchManager {
    private SearchImpl presenterImpl;
    private ArrayList<TruckStop> matchingList;
    private HashMap<Marker, TruckStop> stops;
    private Handler searchBlockHandler;

    public StationSearchManager(SearchImpl presenterImpl) {
        this.presenterImpl = presenterImpl;
        matchingList = new ArrayList<>();
        searchBlockHandler = new Handler();
    }

    private Runnable turnSearchOffRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.d("Search block turn off by runnable");
            presenterImpl.turnSearchBlockOff();
        }
    };

    public void manageSearchBlockRunnable(int delay){
        searchBlockHandler.removeCallbacks(turnSearchOffRunnable);
        searchBlockHandler.postDelayed(turnSearchOffRunnable, delay);
    }

    public void clearResults(){
        matchingList.clear();
    }

    // determine which params are part of search query
    public void determineSearchParams(String name, String city, String state, String zip) {
        stops = presenterImpl.getMarkerMap();

        if (!stops.isEmpty()) {

            boolean includeName = false;
            boolean includeCity = false;
            boolean includeState = false;
            boolean includeZip = false;

            if (!name.trim().isEmpty())
                includeName = true;

            if (!city.trim().isEmpty())
                includeCity = true;

            if (!state.trim().isEmpty())
                includeState = true;

            if (!zip.trim().isEmpty())
                includeZip = true;

            // bitwise track search queries for now - investigate ways to optimize this search
            int digit = (includeZip ? 8 : 0)
                    | (includeState ? 4 : 0)
                    | (includeCity ? 2 : 0)
                    | (includeName ? 1 : 0);

            if (digit >= 0) {
                findMatches(digit, name, city, state, zip);
            }
        }
    }

    private void findMatches(int digit, String name, String city, String state, String zip) {
        for (Map.Entry<Marker, TruckStop> entry : stops.entrySet()) {
            switch (digit) {
                case 0: // do nothing - they asked for literally nothing
                    break;
                case 1: // just name
                    if (justNameMatch(name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 2: // just city
                    if (justCityMatch(city, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 3: // city + name
                    if (city_NameMatch(city, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 4: // just state
                    if (justStateMatch(state, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 5: // state + name
                    if (state_NameMatch(state, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 6: // state + city
                    if (state_cityMatch(state, city, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 7: // state + city + name
                    if (state_city_nameMatch(state, city, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 8: // just zip
                    if (justZipMatch(zip, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 9: // zip + name
                    if (zip_nameMatch(zip, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 10: // zip + city
                    if (zip_cityMatch(zip, city, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 11: // zip + city + name
                    if (zip_city_nameMatch(zip, city, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 12: // zip + state
                    if (zip_stateMatch(zip, state, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 13: // zip + state + name
                    if (zip_state_nameMatch(zip, state, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 14: // zip + state + city
                    if (zip_state_cityMatch(zip, state, city, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
                case 15: // all of them
                    if (allMatch(zip, state, city, name, entry.getValue()))
                        matchingList.add(entry.getValue());
                    break;
            }
        }

        deliverSearchResults();
    }

    private void deliverSearchResults() {
        if (matchingList != null && !matchingList.isEmpty()) {
            presenterImpl.deliverSearchResults(matchingList);
        }
    }

    // handles comparing requested name value with truck stop entry in map set
    private boolean nameCompare(String name, TruckStop stop) {
        return stop.getName() != null &&
                Pattern.compile(Pattern.quote(name),
                        Pattern.CASE_INSENSITIVE).matcher(stop.getName()).find();
    }

    private boolean cityCompare(String city, TruckStop stop) {
        return stop.getCity() != null && city.toLowerCase().equals(stop.getCity().toLowerCase());
    }

    private boolean stateCompare(String state, TruckStop stop) {
        return stop.getState() != null &&
                state.toLowerCase().equals(stop.getState().toLowerCase());
    }

    private boolean zipCompare(String zip, TruckStop stop) {
        return stop.getZip() != null && zip.toLowerCase().equals(stop.getZip().toLowerCase());
    }

    private boolean justNameMatch(String name, TruckStop stop) {
        Timber.d("Matching: name");
        return nameCompare(name, stop);
    }

    private boolean justCityMatch(String city, TruckStop stop) {
        Timber.d("Matching: city");
        return cityCompare(city, stop);
    }

    private boolean city_NameMatch(String city, String name, TruckStop stop) {
        Timber.d("Matching: city, name");
        return cityCompare(city, stop) && nameCompare(name, stop);
    }

    private boolean justStateMatch(String state, TruckStop stop) {
        Timber.d("Matching: state");
        return stateCompare(state, stop);
    }

    private boolean state_NameMatch(String state, String name, TruckStop stop) {
        Timber.d("Matching: state, name");
        return stateCompare(state, stop) && nameCompare(name, stop);
    }

    private boolean state_cityMatch(String state, String city, TruckStop stop) {
        Timber.d("Matching: state, city");
        return stateCompare(state, stop) && cityCompare(city, stop);
    }

    private boolean state_city_nameMatch(String state, String city, String name, TruckStop stop) {
        Timber.d("Matching: state, city, name");
        return stateCompare(state, stop) && cityCompare(city, stop) && nameCompare(name, stop);
    }

    private boolean justZipMatch(String zip, TruckStop stop) {
        Timber.d("Matching: zip");
        return zipCompare(zip, stop);
    }

    private boolean zip_nameMatch(String zip, String name, TruckStop stop) {
        Timber.d("Matching: zip, name");
        return zipCompare(zip, stop) && nameCompare(name, stop);
    }

    private boolean zip_cityMatch(String zip, String city, TruckStop stop) {
        Timber.d("Matching: zip, city");
        return zipCompare(zip, stop) && cityCompare(city, stop);
    }

    private boolean zip_city_nameMatch(String zip, String city, String name, TruckStop stop) {
        Timber.d("Matching: zip, city, name");
        return zipCompare(zip, stop) && cityCompare(city, stop) && nameCompare(name, stop);
    }

    private boolean zip_stateMatch(String zip, String state, TruckStop stop) {
        Timber.d("Matching: zip, state");
        return zipCompare(zip, stop) && stateCompare(state, stop);
    }

    private boolean zip_state_nameMatch(String zip, String state, String name, TruckStop stop) {
        Timber.d("Matching: zip, state, name");
        return zipCompare(zip, stop) && stateCompare(state, stop) && nameCompare(name, stop);
    }

    private boolean zip_state_cityMatch(String zip, String state, String city, TruckStop stop) {
        Timber.d("Matching: zip, state, city");
        return zipCompare(zip, stop) && stateCompare(state, stop) && cityCompare(city, stop);
    }

    private boolean allMatch(String zip, String state, String city, String name, TruckStop stop) {
        Timber.d("Matching: all");
        return zipCompare(zip, stop) && stateCompare(state, stop) && cityCompare(city, stop)
                && nameCompare(name, stop);
    }
}
