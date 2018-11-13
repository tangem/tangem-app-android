package com.tangem.di;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

@Singleton
public class AppModule {
    private Context appContext;

    public AppModule(@NotNull Context context) {
        appContext = context;
    }

    Context provideContext() {
        return appContext;
    }

}