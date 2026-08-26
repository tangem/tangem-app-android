package com.tangem.blockchainsdk.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.WalletManagerFactoryCreator
import com.tangem.blockchainsdk.accountcreator.DefaultAccountCreator
import com.tangem.blockchainsdk.datastorage.DefaultBlockchainDataStorage
import com.tangem.blockchainsdk.providers.BlockchainProviderTypesStore
import com.tangem.blockchainsdk.providers.BlockchainProvidersApi
import com.tangem.blockchainsdk.providers.BlockchainProvidersStorage
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.DefaultBlockchainProvidersStorage
import com.tangem.blockchainsdk.providers.DevBlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.ProdBlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.dev.BlockchainProvidersResponseSerializer
import com.tangem.blockchainsdk.providers.models.ProviderModel
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.store.datasource.api.TangemTech
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.libs.blockchain_sdk.BuildConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockchainSDKFactoryModule {

    @Provides
    @Singleton
    fun provideBlockchainProvidersStorage(assetLoader: AssetLoader): BlockchainProvidersStorage {
        return DefaultBlockchainProvidersStorage(
            assetLoader = assetLoader,
            runtimeStateStore = RuntimeStateStore(defaultValue = emptyMap()),
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainProvidersApi(factory: RetrofitFactory): BlockchainProvidersApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemTech.ID,
                shouldApplyTimeoutAnnotations = true,
                shouldUseSessionAuth = false,
            ),
        )
    }

    @Provides
    @IntoSet
    fun provideProviderModelMoshiConfigurer(): NetworkMoshiConfigurer {
        return NetworkMoshiConfigurer { builder ->
            builder.add(
                PolymorphicJsonAdapterFactory.of(ProviderModel::class.java, "type")
                    .withSubtype(ProviderModel.Public::class.java, "public")
                    .withSubtype(ProviderModel.Private::class.java, "private")
                    .withDefaultValue(ProviderModel.UnsupportedType),
            )
        }
    }

    @Provides
    @Singleton
    fun provideBlockchainSDKFactory(
        environmentConfig: EnvironmentConfig,
        blockchainProvidersTypesManager: BlockchainProvidersTypesManager,
        walletManagerFactoryCreator: WalletManagerFactoryCreator,
        dispatchers: CoroutineDispatcherProvider,
    ): BlockchainSDKFactory {
        return DefaultBlockchainSDKFactory(
            blockchainSdkConfig = environmentConfig.blockchainSdkConfig,
            blockchainProvidersTypesManager = blockchainProvidersTypesManager,
            walletManagerFactoryCreator = walletManagerFactoryCreator,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainProvidersTypesManager(
        prodBlockchainProvidersTypesManager: ProdBlockchainProvidersTypesManager,
        blockchainProviderTypesStore: BlockchainProviderTypesStore,
        changedBlockchainProvidersStore: DataStore<BlockchainProvidersResponse>,
    ): BlockchainProvidersTypesManager {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevBlockchainProvidersTypesManager(
                prodBlockchainProvidersTypesManager = prodBlockchainProvidersTypesManager,
                blockchainProviderTypesStore = blockchainProviderTypesStore,
                changedBlockchainProvidersStore = changedBlockchainProvidersStore,
            )
        } else {
            prodBlockchainProvidersTypesManager
        }
    }

    @Provides
    @Singleton
    fun provideChangedBlockchainProvidersResponseDataStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): DataStore<BlockchainProvidersResponse> {
        return DataStoreFactory.create(
            serializer = BlockchainProvidersResponseSerializer(moshi),
            produceFile = { context.dataStoreFile("changed_providers") },
            scope = appScope,
        )
    }

    @Provides
    @Singleton
    fun provideWalletManagerFactoryCreator(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        blockchainSDKLogger: BlockchainSDKLogger,
        featureTogglesManager: FeatureTogglesManager,
    ): WalletManagerFactoryCreator {
        return WalletManagerFactoryCreator(
            accountCreator = DefaultAccountCreator(tangemTechApi),
            blockchainDataStorage = DefaultBlockchainDataStorage(appPreferencesStore),
            blockchainSDKLogger = blockchainSDKLogger,
            featureToggleValues = WalletManagerFactoryCreator.FeatureToggleValues(
                isYieldModeSwapEnabled = true,
                isXrpTxHistoryEnabled = featureTogglesManager.isFeatureEnabled(
                    FeatureToggles.AND_14786_XRP_TX_HISTORY_ENABLED,
                ),
            ),
        )
    }
}