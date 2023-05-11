package com.tangem.feature.onboarding.data.di

import com.tangem.datasource.asset.AssetReader
import com.tangem.feature.onboarding.data.DefaultMnemonicRepository
import com.tangem.feature.onboarding.data.MnemonicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class OnboardingDataModule {

    @Provides
    @Singleton
    fun provideSeedPhraseSdkRepository(assetReader: AssetReader): MnemonicRepository {
        return DefaultMnemonicRepository(assetReader)
    }
}