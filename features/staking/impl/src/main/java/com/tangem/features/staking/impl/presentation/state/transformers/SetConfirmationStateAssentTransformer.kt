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
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState

@Suppress("UnusedPrivateMember")
internal class SetConfirmationStateAssentTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val stakingGasEstimate: StakingGasEstimate,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(stakingGasEstimate),
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(
        gasEstimate: StakingGasEstimate,
    ): StakingStates.ConfirmationState {
        if (this is StakingStates.ConfirmationState.Data) {
            return copy(
                innerState = InnerConfirmationStakingState.ASSENT,
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
                validatorState = validatorState.copySealed(isClickable = true),
                isPrimaryButtonEnabled = true,
            )
        } else {
            return this
        }
    }
}
