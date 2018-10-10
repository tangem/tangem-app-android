package com.tangem;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import com.tangem.di.AppComponent;
import com.tangem.di.DaggerAppComponent;

public class App extends Application {

    /**
     * A singleton instance of the application class for easy access in other places
     */
    private static App sInstance;

    public App() {
        super();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize the singleton
        sInstance = this;

        component = DaggerAppComponent.create();
    }

    /**
     * @return singleton instance
     */
    public static synchronized App getInstance() {
        return sInstance;
    }

    public static AppComponent getComponent() {
        return component;
    }

}