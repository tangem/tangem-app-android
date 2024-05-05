package com.tangem.blockchainsdk.di

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.WalletManagerFactoryCreator
import com.tangem.blockchainsdk.accountcreator.DefaultAccountCreator
import com.tangem.blockchainsdk.datastorage.DefaultBlockchainDataStorage
import com.tangem.blockchainsdk.loader.BlockchainProvidersResponseLoader
import com.tangem.blockchainsdk.store.DefaultRuntimeStore
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.preferences.AppPreferencesStore
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
        assetLoader: AssetLoader,
        blockchainProvidersResponseLoader: BlockchainProvidersResponseLoader,
        walletManagerFactoryCreator: WalletManagerFactoryCreator,
        dispatchers: CoroutineDispatcherProvider,
    ): BlockchainSDKFactory {
        return DefaultBlockchainSDKFactory(
            assetLoader = assetLoader,
            blockchainProvidersResponseLoader = blockchainProvidersResponseLoader,
            configStore = DefaultRuntimeStore(defaultValue = BlockchainSdkConfig()),
            blockchainProviderTypesStore = DefaultRuntimeStore(defaultValue = emptyMap()),
            walletManagerFactoryCreator = walletManagerFactoryCreator,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideWalletManagerFactoryCreator(
        authProvider: AuthProvider,
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        blockchainSDKLogger: BlockchainSDKLogger,
    ): WalletManagerFactoryCreator {
        return WalletManagerFactoryCreator(
            accountCreator = DefaultAccountCreator(authProvider, tangemTechApi),
            blockchainDataStorage = DefaultBlockchainDataStorage(appPreferencesStore),
            blockchainSDKLogger = blockchainSDKLogger,
        )
    }
}
