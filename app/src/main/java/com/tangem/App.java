package com.tangem;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tangem.tangemserver.android.data.LocalStorage;
import com.tangem.di.DaggerNavigatorComponent;
import com.tangem.di.DaggerNetworkComponent;
import com.tangem.di.NavigatorComponent;
import com.tangem.di.NetworkComponent;
import com.tangem.tangemcard.data.Issuer;
import com.tangem.tangemcard.android.data.Firmwares;
import com.tangem.tangemcard.android.data.PINStorage;
import com.tangem.tangemcard.data.external.FirmwaresDigestsProvider;
import com.tangem.tangemcard.data.external.PINsProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;


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
    private static NavigatorComponent navigatorComponent;

    public static NavigatorComponent getNavigatorComponent() {
        return navigatorComponent;
    }

    public static LocalStorage localStorage;
    public static PINsProvider pinStorage;
    public static FirmwaresDigestsProvider firmwaresStorage;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize the singleton
        sInstance = this;

        networkComponent = DaggerNetworkComponent.create();
        navigatorComponent = buildNavigatorComponent();

        // common init
        if (PINStorage.needInit())
            PINStorage.init(getApplicationContext());

        initIssuers();

        firmwaresStorage = new Firmwares(getApplicationContext());

        localStorage = new LocalStorage(getApplicationContext());

        pinStorage = new PINStorage();
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

    protected NavigatorComponent buildNavigatorComponent() {
        return DaggerNavigatorComponent.builder()
                .build();
    }


    public void initIssuers() {
        try {
            try (InputStream is = getApplicationContext().getAssets().open("issuers.json")) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<List<Issuer>>() {
                    }.getType();


                    Issuer.fillIssuers(new Gson().fromJson(reader, listType));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}