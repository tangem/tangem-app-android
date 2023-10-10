package com.tangem.datasource.di

import android.content.Context
import com.tangem.datasource.local.datastore.BooleanSharedPreferencesDataStore
import com.tangem.datasource.local.settings.*
import com.tangem.datasource.local.userwallet.DefaultShouldSaveUserWalletStore
import com.tangem.datasource.local.userwallet.ShouldSaveUserWalletStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDataModule {

    @Provides
    @Singleton
    fun provideShouldSaveUserWalletsStore(@ApplicationContext context: Context): ShouldSaveUserWalletStore {
        return DefaultShouldSaveUserWalletStore(
            store = BooleanSharedPreferencesDataStore(preferencesName = "tapPrefs", context = context),
        )
    }
}