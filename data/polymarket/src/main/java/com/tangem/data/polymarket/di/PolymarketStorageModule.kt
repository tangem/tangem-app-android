package com.tangem.data.polymarket.di

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.dataStoreFile
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.cleaner.PolymarketUserWalletDataCleaner
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.data.polymarket.sessionkey.DefaultPolymarketDeviceIdProvider
import com.tangem.data.polymarket.sessionkey.DefaultPolymarketSessionCredentialsProvider
import com.tangem.data.polymarket.sessionkey.DefaultPolymarketSessionKeyStore
import com.tangem.data.polymarket.store.DefaultPolymarketCredentialsStore
import com.tangem.data.polymarket.converter.PredictionAccountStatusValueDTOConverter
import com.tangem.data.polymarket.store.DefaultPolymarketOnboardedStore
import com.tangem.data.polymarket.store.DefaultPredictionWithdrawalStore
import com.tangem.data.polymarket.entity.PredictionAccountStatusValueDTO
import com.tangem.data.polymarket.entity.PolymarketSessionKeyReadinessDTO
import com.tangem.data.polymarket.entity.PredictionWithdrawalRecordDTO
import com.tangem.data.polymarket.store.DefaultPolymarketSessionKeyTradingStore
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.sdk.storage.AndroidSecureStorageV2
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
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
        collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    ): PredictionAccountStatusStore {
        return PredictionAccountStatusStore(
            dtoConverter = PredictionAccountStatusValueDTOConverter(
                collateralCurrencyFactory = collateralCurrencyFactory,
            ),
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = dataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = PredictionAccountStatusValueDTO.serializer(),
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
    @Singleton
    fun providePolymarketOnboardedStore(
        @ApplicationContext context: Context,
        scope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): PolymarketOnboardedStore {
        return DefaultPolymarketOnboardedStore(
            dataStore = dataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptySet(),
                    serializer = SetSerializer(String.serializer()),
                ),
                produceFile = { context.dataStoreFile(fileName = "polymarket_onboarded_wallets") },
                scope = scope,
            ),
        )
    }

    @Provides
    @Singleton
    fun providePredictionWithdrawalStore(
        @ApplicationContext context: Context,
        scope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): PredictionWithdrawalStore {
        return DefaultPredictionWithdrawalStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = dataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = PredictionWithdrawalRecordDTO.serializer(),
                    ),
                ),
                produceFile = { context.dataStoreFile(fileName = "prediction_withdrawals") },
                scope = scope,
            ),
            scope = scope,
        )
    }

    @Provides
    @Singleton
    fun providePolymarketSessionKeyStore(
        @ApplicationContext context: Context,
        tangemHotSdk: TangemHotSdk,
        addressFactory: PolymarketAddressFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): PolymarketSessionKeyStore {
        return DefaultPolymarketSessionKeyStore(
            tangemHotSdk = tangemHotSdk,
            secureStorage = AndroidSecureStorageV2(
                appContext = context,
                useStrongBox = false,
                name = "polymarket_session_key_storage",
            ),
            addressFactory = addressFactory,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providePolymarketSessionKeyTradingStore(
        @ApplicationContext context: Context,
        scope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): PolymarketSessionKeyTradingStore {
        return DefaultPolymarketSessionKeyTradingStore(
            readinessDataStore = dataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = PolymarketSessionKeyReadinessDTO.serializer(),
                    ),
                ),
                produceFile = { context.dataStoreFile(fileName = "polymarket_session_key_readiness") },
                scope = scope,
            ),
        )
    }

    @SuppressLint("HardwareIds")
    @Provides
    @Singleton
    fun providePolymarketDeviceIdProvider(
        @ApplicationContext context: Context,
        appInfoProvider: AppInfoProvider,
        dispatchers: CoroutineDispatcherProvider,
    ): PolymarketDeviceIdProvider {
        return DefaultPolymarketDeviceIdProvider(
            androidId = { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) },
            appInfoProvider = appInfoProvider,
            secureStorage = AndroidSecureStorageV2(
                appContext = context,
                useStrongBox = false,
                name = "polymarket_device_id_storage",
            ),
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providePolymarketSessionCredentialsProvider(
        impl: DefaultPolymarketSessionCredentialsProvider,
    ): PolymarketSessionCredentialsProvider = impl

    @Provides
    @IntoSet
    fun providePolymarketUserWalletDataCleaner(impl: PolymarketUserWalletDataCleaner): UserWalletDataCleaner = impl
}