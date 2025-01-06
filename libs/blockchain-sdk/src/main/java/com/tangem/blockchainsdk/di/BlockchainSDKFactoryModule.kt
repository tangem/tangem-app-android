package com.tangem.blockchainsdk.di

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.WalletManagerFactoryCreator
import com.tangem.blockchainsdk.accountcreator.DefaultAccountCreator
import com.tangem.blockchainsdk.datastorage.DefaultBlockchainDataStorage
import com.tangem.blockchainsdk.featuretoggles.DefaultBlockchainSDKFeatureToggles
import com.tangem.blockchainsdk.providers.BlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.DevBlockchainProvidersTypesManager
import com.tangem.blockchainsdk.providers.ProdBlockchainProvidersTypesManager
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.libs.blockchain_sdk.BuildConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
        devBlockchainProvidersTypesManager: DevBlockchainProvidersTypesManager,
        prodBlockchainProvidersTypesManager: ProdBlockchainProvidersTypesManager,
    ): BlockchainProvidersTypesManager {
        return if (BuildConfig.DEBUG) {
            devBlockchainProvidersTypesManager
        } else {
            prodBlockchainProvidersTypesManager
        }
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
            blockchainSDKFeatureToggles = DefaultBlockchainSDKFeatureToggles(featureTogglesManager),
        )
    }
}