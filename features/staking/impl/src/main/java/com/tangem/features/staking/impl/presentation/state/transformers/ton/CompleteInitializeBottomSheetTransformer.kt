package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class CompleteInitializeBottomSheetTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val prevBottomSheetConfig = prevState.bottomSheetConfig ?: return prevState
        val prevBottomSheetConfigContent =
            prevBottomSheetConfig.content as? TonInitializeAccountBottomSheetConfig ?: return prevState

        val feeAmount = (prevBottomSheetConfigContent.feeState as? FeeState.Content)
            ?.fee?.amount?.value?.multiply(BigDecimal(FEE_MULTIPLIER)) ?: return prevState

        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        return prevState.copy(
            amountState = AmountReduceByTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = ReduceByData(
                    feeAmount,
                    feeAmount,
                ),
            ).transform(prevState.amountState),
            confirmationState = confirmationState.copy(
                reduceAmountBy = feeAmount,
            ),
            bottomSheetConfig = prevBottomSheetConfig.copy(
                content = prevBottomSheetConfigContent.copy(
                    isButtonLoading = true,
                    isButtonEnabled = false,
                ),
            ),
        )
    }

    private companion object {
        const val FEE_MULTIPLIER = 1.1
    }
}