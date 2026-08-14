package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.utils.Provider
import javax.inject.Inject

internal class StakingOperationsFactory @Inject constructor(
    private val stakeKitFeeLoaderFactory: StakeKitFeeLoader.Factory,
    private val p2pEthPoolFeeLoaderFactory: P2PEthPoolFeeLoader.Factory,
    private val stakeKitTransactionSenderFactory: StakeKitTransactionSender.Factory,
    private val p2pEthPoolTransactionSenderFactory: P2PEthPoolTransactionSender.Factory,
) {

    fun createFeeLoader(
        cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
        userWallet: UserWallet,
        integration: StakingIntegration,
    ): StakingFeeLoader {
        return when (integration) {
            is StakeKitIntegration -> stakeKitFeeLoaderFactory.create(
                cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
                userWallet = userWallet,
                integration = integration,
            )
            is P2PEthPoolIntegration -> p2pEthPoolFeeLoaderFactory.create(
                cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
                integration = integration,
            )
        }
    }

    fun createTransactionSender(
        cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
        userWallet: UserWallet,
        integration: StakingIntegration,
        isAmountSubtractAvailable: Boolean,
    ): StakingTransactionSender {
        return when (integration) {
            is StakeKitIntegration -> stakeKitTransactionSenderFactory.create(
                cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
                userWallet = userWallet,
                integration = integration,
                isAmountSubtractAvailable = isAmountSubtractAvailable,
            )
            is P2PEthPoolIntegration -> p2pEthPoolTransactionSenderFactory.create(
                cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
                userWallet = userWallet,
                integration = integration,
            )
        }
    }
}