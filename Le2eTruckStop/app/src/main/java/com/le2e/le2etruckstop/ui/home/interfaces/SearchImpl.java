package com.le2e.le2etruckstop.ui.home.interfaces;


import com.le2e.le2etruckstop.data.remote.response.TruckStop;

import java.util.ArrayList;

public interface SearchImpl {
    void deliverSearchResults(ArrayList<TruckStop> results);

    void turnSearchBlockOff();
}
