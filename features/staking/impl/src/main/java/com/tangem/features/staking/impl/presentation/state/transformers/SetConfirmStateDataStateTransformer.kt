package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.transaction.StakingGasEstimate
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer
import com.tangem.blockchain.common.Amount

@Suppress("UnusedPrivateMember")
internal class SetConfirmStateDataStateTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val stakingGasEstimate: StakingGasEstimate,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmStakingState = prevState.confirmStakingState.copyWrapped(stakingGasEstimate),
        )
    }

    private fun StakingStates.ConfirmStakingState.copyWrapped(
        gasEstimate: StakingGasEstimate,
    ): StakingStates.ConfirmStakingState {
        if (this is StakingStates.ConfirmStakingState.Data) {
            return copy(
                feeState = FeeState.Content(
                    fee = Fee.Common(
                        Amount(
                            currencySymbol = gasEstimate.token.symbol,
                            value = gasEstimate.amount,
                            decimals = gasEstimate.token.decimals,
                        ),
                    ),
                    rate = cryptoCurrencyStatusProvider().value.fiatRate,
                    isFeeConvertibleToFiat = cryptoCurrencyStatusProvider().currency.network.hasFiatFeeRate,
                    appCurrency = appCurrencyProvider(),
                    isFeeApproximate = false,
                ),
            )
        } else {
            return this
        }
    }
}