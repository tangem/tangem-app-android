package com.tangem.feature.qrscanning.di

import com.tangem.common.routing.AppRouter
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.feature.qrscanning.navigation.DefaultQrScanningRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object QrScanningRouterModule {

    @Provides
    @ActivityScoped
    fun provideQrScanRouter(appRouter: AppRouter): QrScanningRouter {
        return DefaultQrScanningRouter(appRouter)
    }
}
