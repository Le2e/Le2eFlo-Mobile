package com.le2e.le2etruckstop.injection.component;

import com.le2e.le2etruckstop.injection.module.DataModule;
import com.le2e.le2etruckstop.ui.home_screen.MapsHomeActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {DataModule.class})
public interface PresenterComponent {
    void inject(MapsHomeActivity mapsHomeActivity);
}
