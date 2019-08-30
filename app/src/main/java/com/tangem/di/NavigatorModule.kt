package com.tangem.di

import com.tangem.ui.dialog.WaitSecurityDelayDialogNew
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class NavigatorModule {

    @Singleton
    @Provides
    fun provideToastHelper(): ToastHelper {
        return ToastHelper()
    }

    @Singleton
    @Provides
    fun provideWaitSecurityDelayDialogNew(): WaitSecurityDelayDialogNew {
        return WaitSecurityDelayDialogNew()
    }

}