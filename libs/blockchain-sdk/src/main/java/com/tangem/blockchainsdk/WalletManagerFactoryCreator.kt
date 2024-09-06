package com.tangem.blockchainsdk

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.featuretoggles.BlockchainSDKFeatureToggles
import timber.log.Timber
import javax.inject.Inject

/**
 * Creator of [WalletManagerFactory]
 *
 * @property accountCreator        account creator
 * @property blockchainDataStorage blockchain data storage
 * @property blockchainSDKLogger   blockchain SDK logger
 *
* [REDACTED_AUTHOR]
 */
internal class WalletManagerFactoryCreator @Inject constructor(
    private val accountCreator: AccountCreator,
    private val blockchainDataStorage: BlockchainDataStorage,
    private val blockchainSDKLogger: BlockchainSDKLogger,
    @Suppress("UnusedPrivateMember") private val blockchainSDKFeatureToggles: BlockchainSDKFeatureToggles,
) {

    fun create(config: BlockchainSdkConfig, blockchainProviderTypes: BlockchainProviderTypes): WalletManagerFactory {
        Timber.i("Create WalletManagerFactory")

        return WalletManagerFactory(
            config = config,
            blockchainProviderTypes = blockchainProviderTypes,
            accountCreator = accountCreator,
            featureToggles = BlockchainFeatureToggles(isCardanoTokenSupport = true),
            blockchainDataStorage = blockchainDataStorage,
            loggers = listOf(blockchainSDKLogger),
        )
    }
}
