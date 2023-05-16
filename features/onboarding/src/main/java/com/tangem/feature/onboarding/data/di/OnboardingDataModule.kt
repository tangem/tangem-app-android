package com.tangem.feature.onboarding.data.di

import android.app.Application
import com.tangem.crypto.bip39.Wordlist
import com.tangem.feature.onboarding.data.DefaultMnemonicRepository
import com.tangem.feature.onboarding.data.MnemonicRepository
import com.tangem.sdk.extensions.getWordlist
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
    fun provideSeedPhraseSdkRepository(application: Application): MnemonicRepository {
        return DefaultMnemonicRepository(Wordlist.getWordlist(application))
    }
}