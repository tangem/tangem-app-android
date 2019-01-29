package com.tangem.di;

import com.tangem.presentation.dialog.WaitSecurityDelayDialogNew;

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

    @Singleton
    @Provides
    WaitSecurityDelayDialogNew provideWaitSecurityDelayDialogNew() {
        return new WaitSecurityDelayDialogNew();
    }

}