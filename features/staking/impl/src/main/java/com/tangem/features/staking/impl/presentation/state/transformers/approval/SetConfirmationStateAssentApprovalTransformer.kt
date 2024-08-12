package com.tangem.features.staking.impl.presentation.state.transformers.approval

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.transformer.Transformer

internal class SetConfirmationStateAssentApprovalTransformer(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val fee: TransactionFee,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(),
            bottomSheetConfig = null,
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(): StakingStates.ConfirmationState {
        return if (this is StakingStates.ConfirmationState.Data) {
            copy(
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Content(
                    fee = fee.normal,
                    rate = cryptoCurrencyStatusProvider().value.fiatRate,
                    isFeeConvertibleToFiat = cryptoCurrencyStatusProvider().currency.network.hasFiatFeeRate,
                    appCurrency = appCurrencyProvider(),
                    isFeeApproximate = false,
                ),
                validatorState = validatorState.copySealed(isClickable = true),
                isPrimaryButtonEnabled = true,
                isApprovalNeeded = true,
            )
        } else {
            this
        }
    }
}