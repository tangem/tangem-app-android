package com.tangem.data.tokens.di

import com.tangem.data.tokens.store.DefaultTokenReceiveWarningActionStore
import com.tangem.data.tokens.store.TokenReceiveWarningActionStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenReceiveWarningModule {

    @Provides
    @Singleton
    fun provideTokenReceiveWarningStore(dataStoreFactory: AppDataStoreFactory): TokenReceiveWarningActionStore {
        return DefaultTokenReceiveWarningActionStore(
            persistenceStore = dataStoreFactory.create(
                defaultValue = emptySet<String>(),
                fileName = "token_receive_warnings_viewed",
            ),
        )
    }
}