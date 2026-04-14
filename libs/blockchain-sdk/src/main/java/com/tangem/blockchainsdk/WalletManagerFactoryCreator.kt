package com.tangem.blockchainsdk

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.providers.BlockchainProviderTypes
import timber.log.Timber
import javax.inject.Inject

/**
 * Creator of [WalletManagerFactory]
 *
 * @property accountCreator        account creator
 * @property blockchainDataStorage blockchain data storage
 * @property blockchainSDKLogger   blockchain SDK logger
 *
[REDACTED_AUTHOR]
 */
internal class WalletManagerFactoryCreator @Inject constructor(
    private val accountCreator: AccountCreator,
    private val blockchainDataStorage: BlockchainDataStorage,
    private val blockchainSDKLogger: BlockchainSDKLogger,
    private val isSolanaTxHistoryEnabled: Boolean,
    private val isHederaErc20Enabled: Boolean,
) {

    fun create(config: BlockchainSdkConfig, blockchainProviderTypes: BlockchainProviderTypes): WalletManagerFactory {
        Timber.i("Create WalletManagerFactory")

        return WalletManagerFactory(
            config = config,
            blockchainProviderTypes = blockchainProviderTypes,
            accountCreator = accountCreator,
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isPendingTransactionsEnabled = true,
                isSolanaTxHistoryEnabled = isSolanaTxHistoryEnabled,
                isHederaErc20Enabled = isHederaErc20Enabled,
            ),
            blockchainDataStorage = blockchainDataStorage,
            loggers = listOf(blockchainSDKLogger),
        )
    }
}