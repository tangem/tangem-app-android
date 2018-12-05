package com.tangem.tangemcard;

import android.support.v7.app.AppCompatDelegate;

import com.tangem.tangemcard.di.DaggerNetworkComponent;
import com.tangem.tangemcard.di.NetworkComponent;

public class App {
    public App() {
        super();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        networkComponent = DaggerNetworkComponent.create();
    }

    private static NetworkComponent networkComponent;

    public static NetworkComponent getNetworkComponent() {
        return networkComponent;
    }

}