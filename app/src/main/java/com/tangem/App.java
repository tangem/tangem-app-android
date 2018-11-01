package com.tangem;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import com.tangem.di.DaggerNetworkComponent;
import com.tangem.di.NetworkComponent;

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

    private static NetworkComponent networkComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize the singleton
        sInstance = this;

        networkComponent = DaggerNetworkComponent.create();
    }

    /**
     * @return singleton instance
     */
    public static synchronized App getInstance() {
        return sInstance;
    }

    public static NetworkComponent getNetworkComponent() {
        return networkComponent;
    }

}