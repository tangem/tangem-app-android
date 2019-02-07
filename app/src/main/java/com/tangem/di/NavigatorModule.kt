package com.tangem.di

import com.tangem.presentation.dialog.WaitSecurityDelayDialogNew

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

@Module
internal class NavigatorModule {

    @Singleton
    @Provides
    fun provideNavigator(): Navigator {
        return Navigator()
    }

    @Singleton
    @Provides
    fun provideWaitSecurityDelayDialogNew(): WaitSecurityDelayDialogNew {
        return WaitSecurityDelayDialogNew()
    }

}