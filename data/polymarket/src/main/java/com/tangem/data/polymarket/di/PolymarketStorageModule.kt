package com.tangem.data.polymarket.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.cleaner.PolymarketUserWalletDataCleaner
import com.tangem.data.polymarket.store.DefaultPolymarketCredentialsStore
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketStorageModule {

    @Provides
    @Singleton
    fun providePolymarketCredentialsStore(
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): PolymarketCredentialsStore {
        val secureStorage: SecureStorage = AndroidSecureStorageV2(
            appContext = context,
            useStrongBox = false,
            name = "polymarket_credentials_storage",
        )

        return DefaultPolymarketCredentialsStore(
            secureStorage = secureStorage,
            json = Json { ignoreUnknownKeys = true },
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providePredictionAccountStatusStore(
        @ApplicationContext context: Context,
        scope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): PredictionAccountStatusStore {
        return PredictionAccountStatusStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = dataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = PredictionAccountStatusValue.serializer(),
                    ),
                    json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" },
                ),
                produceFile = { context.dataStoreFile(fileName = "prediction_account_statuses") },
                scope = scope,
            ),
            scope = scope,
        )
    }

    @Provides
    @IntoSet
    fun providePolymarketUserWalletDataCleaner(impl: PolymarketUserWalletDataCleaner): UserWalletDataCleaner = impl
}