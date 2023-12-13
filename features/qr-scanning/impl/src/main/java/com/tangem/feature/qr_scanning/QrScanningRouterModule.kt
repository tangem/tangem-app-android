package com.tangem.feature.qr_scanning

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
    fun provideQrScanRouter(): QrScanRouter {
        return DefaultQrScanRouter()
    }
}
