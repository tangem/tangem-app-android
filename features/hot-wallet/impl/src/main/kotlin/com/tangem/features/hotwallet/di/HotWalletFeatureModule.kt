package com.tangem.features.hotwallet.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.hotwallet.DefaultHotWalletFeatureToggles
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.common.repository.DefaultMnemonicRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HotWalletFeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): HotWalletFeatureToggles {
        return DefaultHotWalletFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HotWalletFeatureModuleBinds {
    @Binds
    @Singleton
    fun provideMnemonicRepository(repository: DefaultMnemonicRepository): MnemonicRepository
}