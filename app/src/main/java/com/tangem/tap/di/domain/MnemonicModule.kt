package com.tangem.tap.di.domain

import android.content.Context
import com.tangem.crypto.bip39.Wordlist
import com.tangem.feature.onboarding.data.DefaultMnemonicRepository
import com.tangem.feature.onboarding.data.MnemonicRepository
import com.tangem.sdk.extensions.getWordlist
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MnemonicModule {

    @Provides
    @Singleton
    fun provideMnemonicRepository(@ApplicationContext context: Context): MnemonicRepository {
        return DefaultMnemonicRepository(Wordlist.getWordlist(context))
    }
}