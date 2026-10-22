package com.tangem.features.hotwallet.di

import android.content.Context
import com.tangem.features.hotwallet.DefaultHotWalletFeatureToggles
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HotWalletFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideHotWalletFeatureToggles(@ApplicationContext context: Context): HotWalletFeatureToggles {
        return DefaultHotWalletFeatureToggles(context)
    }
}