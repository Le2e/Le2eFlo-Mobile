package com.le2e.le2etruckstop.config;


import android.app.Application;
import android.content.Context;

import com.le2e.le2etruckstop.BuildConfig;
import com.le2e.le2etruckstop.injection.component.AppComponent;
import com.le2e.le2etruckstop.injection.component.DaggerAppComponent;
import com.le2e.le2etruckstop.injection.module.DataModule;
import com.le2e.le2etruckstop.injection.module.NetworkModule;
import com.le2e.le2etruckstop.injection.module.StationServiceModule;

import timber.log.Timber;

public class BaseApplication extends Application {
    private static Context context;
    private AppComponent appComponent;

    public static BaseApplication get() {
        return (BaseApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BaseApplication.context = getApplicationContext();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        appComponent = DaggerAppComponent.builder()
                .dataModule(new DataModule())
                .stationServiceModule(new StationServiceModule())
                .networkModule(new NetworkModule())
                .build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}
