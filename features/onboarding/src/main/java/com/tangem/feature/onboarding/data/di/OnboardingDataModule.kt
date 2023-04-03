package com.tangem.feature.onboarding.data.di

import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.feature.onboarding.data.DummySeedPhraseRepository
import com.tangem.feature.onboarding.data.DummyWordlist
import com.tangem.feature.onboarding.data.SeedPhraseRepository
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
    fun provideMnemonic(): Mnemonic = DefaultMnemonic(
        entropy = EntropyLength.Bits128Length,
        wordlist = DummyWordlist(),
    )

    @Provides
    @Singleton
    fun provideSeedPhraseSdkRepository(
    ): SeedPhraseRepository {
        return DummySeedPhraseRepository()
    }
}
