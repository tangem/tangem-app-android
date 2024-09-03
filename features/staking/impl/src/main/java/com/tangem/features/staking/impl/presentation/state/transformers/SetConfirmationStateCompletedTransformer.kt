package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateCompletedTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val fee: Fee,
    private val txUrl: String,
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
                isPrimaryButtonEnabled = true,
                innerState = InnerConfirmationStakingState.COMPLETED,
                feeState = FeeState.Content(
                    fee = fee,
                    rate = feeCryptoCurrencyStatus?.value?.fiatRate,
                    isFeeConvertibleToFiat = isFeeConvertibleToFiat,
                    appCurrency = appCurrencyProvider(),
                    isFeeApproximate = false,
                ),
                validatorState = validatorState.copySealed(
                    isClickable = false,
                ),
                notifications = persistentListOf(),
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
