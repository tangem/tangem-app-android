package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.DefaultTangemPayFeatureToggles
import com.tangem.features.tangempay.TangemPayFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemPayDetailsModule {

    @Provides
    @Singleton
    fun provideTangemPayFeatureToggles(): TangemPayFeatureToggles {
        return DefaultTangemPayFeatureToggles()
    }
}