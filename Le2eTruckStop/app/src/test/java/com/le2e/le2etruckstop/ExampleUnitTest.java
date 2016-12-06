package com.le2e.le2etruckstop;

import android.app.Application;
import android.content.SharedPreferences;

import com.le2e.le2etruckstop.data.manager.DataManager;
import com.le2e.le2etruckstop.ui.home.MapsHomePresenter;
import com.le2e.le2etruckstop.ui.home.MapsHomeView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class ExampleUnitTest {
    Application application;
    MapsHomeView homeView;
    MapsHomePresenter presenter;
    DataManager dataManager;
    SharedPreferences sharedPreferences;


    @Before
    public void setup() {
        application = Mockito.mock(Application.class);
        homeView = Mockito.mock(MapsHomeView.class);
        sharedPreferences = Mockito.mock(SharedPreferences.class);
        dataManager = new DataManager(null, sharedPreferences);
        presenter = new MapsHomePresenter(dataManager);
        presenter.attachView(homeView);
    }

    @Test
    public void ensureSetupWasSuccessful(){
        assertTrue(application != null);
        assertTrue(homeView != null);
        assertTrue(sharedPreferences != null);
        assertTrue(dataManager != null);
        assertTrue(presenter != null);
    }

    @Test
    public void ensureToggleTrackingFabIconIsCalled(){
        presenter.toggleTrackingFabIcon(true);
        verify(homeView).toggleTrackingIcon(true);

        presenter.toggleTrackingFabIcon(false);
        verify(homeView).toggleTrackingIcon(false);
    }
}