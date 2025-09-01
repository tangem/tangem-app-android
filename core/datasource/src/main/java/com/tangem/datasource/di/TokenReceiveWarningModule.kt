package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.token.DefaultTokenReceiveWarningActionStore
import com.tangem.datasource.local.token.TokenReceiveWarningActionStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.setTypes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TokenReceiveWarningModule {

    @Provides
    @Singleton
    fun provideTokenReceiveWarningStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): TokenReceiveWarningActionStore {
        return DefaultTokenReceiveWarningActionStore(
            persistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = setTypes<String>(),
                    defaultValue = emptySet(),
                ),
                produceFile = { context.dataStoreFile(fileName = "token_receive_warnings_viewed") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}