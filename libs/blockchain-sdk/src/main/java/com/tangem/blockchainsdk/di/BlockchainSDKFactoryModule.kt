package com.tangem.blockchainsdk.di

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.accountcreator.DefaultAccountCreator
import com.tangem.blockchainsdk.config.RuntimeConfigStorage
import com.tangem.blockchainsdk.datastorage.DefaultBlockchainDataStorage
import com.tangem.blockchainsdk.loader.ConfigLoader
import com.tangem.blockchainsdk.loader.LocalConfigLoader
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.di.SdkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.libs.blockchain_sdk.BuildConfig
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
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
        @SdkMoshi moshi: Moshi,
        assetReader: AssetReader,
        dispatchers: AppCoroutineDispatcherProvider,
        authProvider: AuthProvider,
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        blockchainSDKLogger: BlockchainSDKLogger,
    ): BlockchainSDKFactory {
        return DefaultBlockchainSDKFactory(
            configLoader = createConfigLoader(moshi, assetReader, dispatchers),
            configStorage = RuntimeConfigStorage(),
            accountCreator = DefaultAccountCreator(authProvider, tangemTechApi),
            blockchainDataStorage = DefaultBlockchainDataStorage(appPreferencesStore),
            blockchainSDKLogger = blockchainSDKLogger,
        )
    }

    private fun createConfigLoader(
        moshi: Moshi,
        assetReader: AssetReader,
        dispatchers: AppCoroutineDispatcherProvider,
    ): ConfigLoader {
        return LocalConfigLoader(
            buildEnvironment = BuildConfig.ENVIRONMENT,
            moshi = moshi,
            assetReader = assetReader,
            dispatchers = dispatchers,
        )
    }
}
