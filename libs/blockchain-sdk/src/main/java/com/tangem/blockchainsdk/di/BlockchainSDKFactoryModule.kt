package com.tangem.blockchainsdk.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.WalletManagerFactoryCreator
import com.tangem.blockchainsdk.accountcreator.DefaultAccountCreator
import com.tangem.blockchainsdk.datastorage.DefaultBlockchainDataStorage
import com.tangem.blockchainsdk.providers.BlockchainProviderTypesStore
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.DevBlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.ProdBlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.dev.BlockchainProvidersResponseSerializer
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.libs.blockchain_sdk.BuildConfig
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
internal object BlockchainSDKFactoryModule {

    @Provides
    @Singleton
    fun provideBlockchainSDKFactory(
        blockchainProvidersTypesManager: BlockchainProvidersTypesManager,
        environmentConfigStorage: EnvironmentConfigStorage,
        walletManagerFactoryCreator: WalletManagerFactoryCreator,
        dispatchers: CoroutineDispatcherProvider,
    ): BlockchainSDKFactory {
        return DefaultBlockchainSDKFactory(
            blockchainProvidersTypesManager = blockchainProvidersTypesManager,
            environmentConfigStorage = environmentConfigStorage,
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
        dispatchers: CoroutineDispatcherProvider,
    ): DataStore<BlockchainProvidersResponse> {
        return DataStoreFactory.create(
            serializer = BlockchainProvidersResponseSerializer(moshi),
            produceFile = { context.dataStoreFile("changed_providers") },
            scope = CoroutineScope(dispatchers.io + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideWalletManagerFactoryCreator(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        blockchainSDKLogger: BlockchainSDKLogger,
    ): WalletManagerFactoryCreator {
        return WalletManagerFactoryCreator(
            accountCreator = DefaultAccountCreator(tangemTechApi),
            blockchainDataStorage = DefaultBlockchainDataStorage(appPreferencesStore),
            blockchainSDKLogger = blockchainSDKLogger,
        )
    }
}