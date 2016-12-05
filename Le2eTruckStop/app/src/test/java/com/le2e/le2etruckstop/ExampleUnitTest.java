package com.le2e.le2etruckstop;

import android.app.Application;
import android.app.Instrumentation;
import com.le2e.le2etruckstop.injection.component.AppComponent;
import com.le2e.le2etruckstop.injection.component.DaggerAppComponent;
import com.le2e.le2etruckstop.injection.module.DataModule;
import com.le2e.le2etruckstop.injection.module.NetworkModule;
import com.le2e.le2etruckstop.injection.module.StationServiceModule;
import com.le2e.le2etruckstop.ui.home.MapsHomePresenter;
import com.le2e.le2etruckstop.ui.home.MapsHomeView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class ExampleUnitTest {
    AppComponent appComponent;
    Application mApplication;
    MapsHomeView homeView;
    MapsHomePresenter presenter;

    @Before
    public void setup() {

        appComponent = DaggerAppComponent.builder()
            .dataModule(new DataModule(mApplication))
            .stationServiceModule(new StationServiceModule())
            .networkModule(new NetworkModule())
            .build();

        //presenter = new MapsHomePresenter(mApplication.getAppComponent().getDataManager());
        homeView = Mockito.mock(MapsHomeView.class);
    }

    @Test
    public void mApplicationIsNotNull() {
        assertTrue(mApplication != null);
    }

    @Test
    public void appComponentIsNotNull() {
        assertTrue(appComponent != null);
    }

    @Test
    public void homePresenterIsNotNull() {
        assertTrue(presenter != null);
    }
}