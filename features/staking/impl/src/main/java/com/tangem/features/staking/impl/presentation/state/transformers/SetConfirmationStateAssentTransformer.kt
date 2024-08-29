package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateAssentTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
    private val fee: Fee,
    private val action: PendingAction?,
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
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Content(
                    fee = fee,
                    rate = feeCryptoCurrencyStatus?.value?.fiatRate,
                    isFeeConvertibleToFiat = isFeeConvertibleToFiat,
                    appCurrency = appCurrencyProvider(),
                    isFeeApproximate = false,
                ),
                validatorState = validatorState.copySealed(isClickable = true),
                pendingAction = action,
                isPrimaryButtonEnabled = true,
                isApprovalNeeded = false,
            )
        } else {
            return this
        }
    }
}