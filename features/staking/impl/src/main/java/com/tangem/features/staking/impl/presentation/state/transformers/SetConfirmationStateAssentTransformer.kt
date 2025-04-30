package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateAssentTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val fee: Fee,
    private val isFeeApproximate: Boolean,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(fee),
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(fee: Fee): StakingStates.ConfirmationState {
        if (this is StakingStates.ConfirmationState.Data) {
            val isFeeConvertibleToFiat = feeCryptoCurrencyStatus?.currency?.network?.hasFiatFeeRate == true
            return copy(
                feeState = FeeState.Content(
                    fee = fee,
                    rate = feeCryptoCurrencyStatus?.value?.fiatRate,
                    isFeeConvertibleToFiat = isFeeConvertibleToFiat,
                    appCurrency = appCurrencyProvider(),
                    isFeeApproximate = isFeeApproximate,
                ),
                isPrimaryButtonEnabled = with(cryptoCurrencyStatus.value) {
                    sources.yieldBalanceSource.isActual() && sources.networkSource.isActual()
                },
            )
        } else {
            return this
        }
    }
}