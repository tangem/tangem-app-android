package com.tangem.feature.wallet.di

import com.tangem.feature.wallet.featuretoggles.DefaultWalletFeatureToggles
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletFeatureTogglesModule {

    @Singleton
    @Binds
    fun bindWalletFeatureToggles(toggles: DefaultWalletFeatureToggles): WalletFeatureToggles
}