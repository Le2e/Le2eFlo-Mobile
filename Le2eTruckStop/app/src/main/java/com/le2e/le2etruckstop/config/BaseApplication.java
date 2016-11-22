package com.le2e.le2etruckstop.config;


import android.app.Application;
import android.content.Context;

import com.le2e.le2etruckstop.BuildConfig;
import com.le2e.le2etruckstop.injection.component.DaggerPresenterComponent;
import com.le2e.le2etruckstop.injection.component.PresenterComponent;
import com.le2e.le2etruckstop.injection.module.DataModule;
import com.le2e.le2etruckstop.injection.module.NetworkModule;
import com.le2e.le2etruckstop.injection.module.StationServiceModule;

import timber.log.Timber;

public class BaseApplication extends Application {
    private static Context context;
    private PresenterComponent presenterComponent;

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

        presenterComponent = DaggerPresenterComponent.builder()
                .dataModule(new DataModule())
                .stationServiceModule(new StationServiceModule())
                .networkModule(new NetworkModule())
                .build();
    }

    public PresenterComponent getPresenterComponent() {
        return presenterComponent;
    }
}
