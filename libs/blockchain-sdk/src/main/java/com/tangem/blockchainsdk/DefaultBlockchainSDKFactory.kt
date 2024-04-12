package com.tangem.blockchainsdk

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.config.ConfigStorage
import com.tangem.blockchainsdk.converters.BlockchainSDKConfigConverter
import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.models.ConfigValueModel
import com.tangem.libs.blockchain_sdk.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Implementation of Blockchain SDK components factory
 *
 * @property assetLoader           asset loader
 * @property configStorage         config storage
 * @property accountCreator        account creator
 * @property blockchainDataStorage blockchain data storage
 * @property blockchainSDKLogger   blockchain SDK logger
 *
[REDACTED_AUTHOR]
 */
internal class DefaultBlockchainSDKFactory(
    private val assetLoader: AssetLoader,
    private val configStorage: ConfigStorage,
    private val accountCreator: AccountCreator,
    private val blockchainDataStorage: BlockchainDataStorage,
    private val blockchainSDKLogger: BlockchainSDKLogger,
) : BlockchainSDKFactory {

    override val walletManagerFactory: Flow<WalletManagerFactory> by lazy(::createWalletManagerFactory)

    override suspend fun init() {
        val configValueModel = assetLoader.load<ConfigValueModel>(CONFIG_FILE_NAME) ?: return

        configStorage.store(
            config = BlockchainSDKConfigConverter.convert(value = configValueModel),
        )
    }

    override suspend fun getWalletManagerFactorySync(): WalletManagerFactory? = walletManagerFactory.firstOrNull()

    private fun createWalletManagerFactory(): Flow<WalletManagerFactory> {
        return configStorage.get().map { config ->
            WalletManagerFactory(
                config = config,
                accountCreator = accountCreator,
                blockchainDataStorage = blockchainDataStorage,
                loggers = listOf(blockchainSDKLogger),
            )
        }
    }

    private companion object {

        const val CONFIG_FILE_NAME = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
    }
}