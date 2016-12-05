package com.le2e.le2etruckstop.injection.component;

import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.injection.module.DataModule;
import com.le2e.le2etruckstop.ui.home.MapsHomeActivity;

import dagger.Provides;
import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {DataModule.class})
public interface AppComponent {
    void inject(MapsHomeActivity mapsHomeActivity);

    public DataManager getDataManager();
}