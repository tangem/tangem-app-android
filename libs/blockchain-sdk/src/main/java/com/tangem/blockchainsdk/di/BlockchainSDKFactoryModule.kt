package com.tangem.blockchainsdk.di

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.DefaultBlockchainSDKFactory
import com.tangem.blockchainsdk.config.RuntimeConfigStorage
import com.tangem.blockchainsdk.loader.ConfigLoader
import com.tangem.blockchainsdk.loader.LocalConfigLoader
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.di.SdkMoshi
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
        accountCreator: AccountCreator,
        blockchainDataStorage: BlockchainDataStorage,
        blockchainSDKLogger: BlockchainSDKLogger,
    ): BlockchainSDKFactory {
        return DefaultBlockchainSDKFactory(
            configLoader = createConfigLoader(moshi, assetReader, dispatchers),
            configStorage = RuntimeConfigStorage(),
            accountCreator = accountCreator,
            blockchainDataStorage = blockchainDataStorage,
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
