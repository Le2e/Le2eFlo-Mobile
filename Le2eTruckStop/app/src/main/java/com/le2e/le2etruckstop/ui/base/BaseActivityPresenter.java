package com.le2e.le2etruckstop.ui.base;


import android.app.Activity;

import java.lang.ref.WeakReference;

public abstract class BaseActivityPresenter {
    protected WeakReference<Activity> activityRef;

    public void attachActivity(Activity activity) {
        activityRef = new WeakReference<>(activity);
        setInterface();
    }

    public void detachActivity() {
        activityRef = null;
    }

    protected abstract void setInterface();
}
