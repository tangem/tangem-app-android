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
import java.math.RoundingMode

internal class CompleteInitializeBottomSheetTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val prevBottomSheetConfig = prevState.bottomSheetConfig ?: return prevState
        val prevBottomSheetConfigContent =
            prevBottomSheetConfig.content as? TonInitializeAccountBottomSheetConfig ?: return prevState

        val feeAmount = (prevBottomSheetConfigContent.feeState as? FeeState.Content)?.fee?.amount ?: return prevState

        val multipliedFeeAmount = feeAmount.value?.multiply(BigDecimal(FEE_MULTIPLIER))
            ?.setScale(feeAmount.decimals, RoundingMode.HALF_DOWN) ?: return prevState

        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        return prevState.copy(
            amountState = AmountReduceByTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                minimumTransactionAmount = minimumTransactionAmount,
                value = ReduceByData(
                    multipliedFeeAmount,
                    multipliedFeeAmount,
                ),
            ).transform(prevState.amountState),
            confirmationState = confirmationState.copy(
                reduceAmountBy = multipliedFeeAmount,
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