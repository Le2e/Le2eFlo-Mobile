package com.le2e.le2etruckstop.ui.home_screen;


import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.le2e.le2etruckstop.R;
import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.model.TruckStopUtils;
import com.le2e.le2etruckstop.data.remote.response.StationsResponse;
import com.le2e.le2etruckstop.data.remote.response.TruckStop;
import com.le2e.le2etruckstop.ui.base.mvp.core.MvpBasePresenter;

import java.util.ArrayList;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MapsHomePresenter extends MvpBasePresenter<MapsHomeView> {
    private DataManager dataManager;
    private Subscription truckStationSub;

    public MapsHomePresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    void doStuff(String radius, double lat, double lng) {
        truckStationSub = dataManager.getStationsFromApi(radius, lat, lng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StationsResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(StationsResponse stationsResponse) {
                        stationsRetrieved(stationsResponse.getTruckStopList());
                    }
                });
    }

    public void stationsRetrieved(ArrayList<TruckStop> list) {
        if (isViewAttached()) {
            if (getView() != null) {
                double lat;
                double lng;
                MarkerOptions options = new MarkerOptions();

                for (TruckStop truckStop : list) {
                    lat = Double.parseDouble(truckStop.getLat());
                    lng = Double.parseDouble(truckStop.getLng());
                    options.position(new LatLng(lat, lng));
                    options.title(truckStop.getName());
                    options.snippet(formatRawLine(truckStop.getRawLine1(), truckStop.getRawLine2(), truckStop.getRawLine3()));
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_store_mall_directory_black_36dp));

                    getView().addTruckStopToMap(options);
                }
            }
        }
    }

    private String formatRawLine(String l1, String l2, String l3) {
        return TruckStopUtils.formatTruckStopRawLine(l1, l2, l3);
    }

    public void unSubscribeObserves(){
        truckStationSub.unsubscribe();
    }
}
