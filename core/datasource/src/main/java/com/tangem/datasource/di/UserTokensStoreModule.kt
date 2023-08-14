package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.files.FileReader
import com.tangem.datasource.local.datastore.FileDataStore
import com.tangem.datasource.local.token.DefaultUserTokensStore
import com.tangem.datasource.local.token.UserTokensStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserTokensStoreModule {

    @Provides
    @Singleton
    fun provideUserTokensStore(fileReader: FileReader, @NetworkMoshi moshi: Moshi): UserTokensStore {
        return DefaultUserTokensStore(
            dataStore = FileDataStore(fileReader, moshi.adapter(UserTokensResponse::class.java)),
        )
    }
}