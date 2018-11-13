package com.tangem.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class NavigatorModule {

    @Singleton
    @Provides
    Navigator provideNavigator() {
        return new Navigator();
    }

}