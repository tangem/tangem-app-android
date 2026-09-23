package com.tangem.data.onboarding.di

import com.tangem.core.local.moshi.SdkMoshiConfigurer
import com.tangem.domain.models.scan.serialization.addScanResponseAdapters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Contributes the card-model (ScanResponse) adapters to the shared SDK Moshi, since onboarding
 * persists [com.tangem.domain.models.scan.ScanResponse] through it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingSdkMoshiModule {

    @Provides
    @IntoSet
    fun provideScanResponseSdkMoshiConfigurer(): SdkMoshiConfigurer = SdkMoshiConfigurer { builder ->
        builder.addScanResponseAdapters()
    }
}