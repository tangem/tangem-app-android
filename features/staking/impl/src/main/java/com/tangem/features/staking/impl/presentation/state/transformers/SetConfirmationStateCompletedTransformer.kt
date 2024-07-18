package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.transaction.StakingGasEstimate
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateCompletedTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val stakingGasEstimate: StakingGasEstimate,
    private val txUrl: String,
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
                isPrimaryButtonEnabled = true,
                innerState = InnerConfirmationStakingState.COMPLETED,
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
                validatorState = validatorState.copySealed(
                    isClickable = false,
                ),
                transactionDoneState = TransactionDoneState.Content(
                    timestamp = System.currentTimeMillis(),
                    txUrl = txUrl,
                ),
            )
        } else {
            return this
        }
    }
}
