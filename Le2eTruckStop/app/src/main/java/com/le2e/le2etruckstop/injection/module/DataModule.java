package com.le2e.le2etruckstop.injection.module;

import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.data.remote.request.ApiContentHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = StationServiceModule.class)
public class DataModule {
    @Provides
    @Singleton
    DataManager dataManager(ApiContentHelper apiContentHelper){
        return new DataManager(apiContentHelper);
    }
}
