package com.le2e.le2etruckstop.injection.module;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.remote.request.ApiContentHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = StationServiceModule.class)
public class DataModule {
    private final Application application;

    public DataModule(Application application){
        this.application = application;
    }

    @Provides
    @Singleton
    DataManager dataManager(ApiContentHelper apiContentHelper, SharedPreferences sharedPreferences){
        return new DataManager(apiContentHelper, sharedPreferences);
    }

    @Provides
    @Singleton
    SharedPreferences getSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(application);
    }
}
