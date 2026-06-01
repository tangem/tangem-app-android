package com.tangem.blockchainsdk

import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.blockchainsdk.providers.BlockchainProviderTypes
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Creator of [WalletManagerFactory]
 *
 * @property accountCreator        account creator
 * @property blockchainDataStorage blockchain data storage
 * @property blockchainSDKLogger   blockchain SDK logger
 * @property featureToggleValues   blockchain feature toggle values
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class WalletManagerFactoryCreator @Inject constructor(
    private val accountCreator: AccountCreator,
    private val blockchainDataStorage: BlockchainDataStorage,
    private val blockchainSDKLogger: BlockchainSDKLogger,
    private val featureToggleValues: FeatureToggleValues,
) {

    fun create(config: BlockchainSdkConfig, blockchainProviderTypes: BlockchainProviderTypes): WalletManagerFactory {
        TangemLogger.i("Create WalletManagerFactory")

        return WalletManagerFactory(
            config = config,
            blockchainProviderTypes = blockchainProviderTypes,
            accountCreator = accountCreator,
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isYieldModeSwapEnabled = featureToggleValues.isYieldModeSwapEnabled,
                isPendingTransactionsEnabled = true,
                isSolanaTxHistoryEnabled = featureToggleValues.isSolanaTxHistoryEnabled,
                isSolanaScaledUiAmountEnabled = featureToggleValues.isSolanaScaledUiAmountEnabled,
                isHederaErc20Enabled = featureToggleValues.isHederaErc20Enabled,
                isStateOverrideGasEstimateEnabled = featureToggleValues.isStateOverrideGasEstimateEnabled,
            ),
            blockchainDataStorage = blockchainDataStorage,
            loggers = listOf(blockchainSDKLogger),
        )
    }

    data class FeatureToggleValues(
        val isSolanaTxHistoryEnabled: Boolean,
        val isSolanaScaledUiAmountEnabled: Boolean,
        val isYieldModeSwapEnabled: Boolean,
        val isHederaErc20Enabled: Boolean,
        val isStateOverrideGasEstimateEnabled: Boolean,
    )
}