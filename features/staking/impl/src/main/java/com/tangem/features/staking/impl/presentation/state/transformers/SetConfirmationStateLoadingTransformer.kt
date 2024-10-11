package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.getRewardSchedule
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateLoadingTransformer(
    private val yield: Yield,
    private val appCurrency: AppCurrency,
    private val cryptoCurrency: CryptoCurrency,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        return prevState.copy(
            confirmationState = StakingStates.ConfirmationState.Data(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Loading,
                notifications = persistentListOf(),
                footerText = getFooter(prevState),
                transactionDoneState = TransactionDoneState.Empty,
                pendingAction = possibleConfirmationState?.pendingAction,
                pendingActions = possibleConfirmationState?.pendingActions,
                isApprovalNeeded = false,
                reduceAmountBy = null,
                possiblePendingTransaction = null,
            ),
        )
    }

    private fun getFooter(state: StakingUiState): TextReference {
        val amountState = state.amountState as? AmountState.Data

        val isEnterAction = state.actionType == StakingActionCommonType.ENTER

        val amountDecimal = amountState?.amountTextField?.fiatAmount?.value
        val amountValue = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = amountDecimal,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
        val rewardSchedule = getRewardSchedule(
            yield.metadata.rewardSchedule,
            cryptoCurrency.network.id.value,
        )
        return if (isEnterAction && amountDecimal != null && rewardSchedule != null) {
            resourceReference(
                id = R.string.staking_summary_description_text,
                formatArgs = wrappedList(amountValue, rewardSchedule),
            )
        } else {
            TextReference.EMPTY
        }
    }
}
