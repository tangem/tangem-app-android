package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateLoadingTransformer(
    private val yield: Yield,
    private val appCurrency: AppCurrency,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val filteredValidators = yield.validators.filter {
            it.preferred
        }
        val possibleConfirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val possibleValidatorState = possibleConfirmationState?.validatorState as? ValidatorState.Content
        val chosenValidator = possibleValidatorState?.chosenValidator ?: filteredValidators[0]

        return prevState.copy(
            confirmationState = StakingStates.ConfirmationState.Data(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Loading,
                validatorState = ValidatorState.Content(
                    isClickable = true,
                    chosenValidator = chosenValidator,
                    availableValidators = filteredValidators,
                ),
                notifications = persistentListOf(),
                footerText = getFooter(prevState, chosenValidator),
                transactionDoneState = TransactionDoneState.Empty,
                pendingAction = possibleConfirmationState?.pendingAction,
                pendingActions = possibleConfirmationState?.pendingActions,
                isApprovalNeeded = false,
                reduceAmountBy = null,
                balanceState = null,
            ),
        )
    }

    private fun getFooter(state: StakingUiState, validator: Yield.Validator): TextReference {
        val amountState = state.amountState as? AmountState.Data

        val isEnterAction = state.actionType == StakingActionCommonType.ENTER

        val apr = validator.apr.orZero()
        val amountDecimal = amountState?.amountTextField?.fiatAmount?.value
        val potentialReward = amountDecimal?.multiply(apr)

        val amountValue = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = amountDecimal,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
        val potentialRewardValue = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = potentialReward,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
            withApproximateSign = true,
        )
        return if (isEnterAction && amountDecimal != null && potentialReward != null) {
            resourceReference(
                id = R.string.staking_summary_description_text,
                formatArgs = wrappedList(amountValue, potentialRewardValue),
            )
        } else {
            TextReference.EMPTY
        }
    }
}