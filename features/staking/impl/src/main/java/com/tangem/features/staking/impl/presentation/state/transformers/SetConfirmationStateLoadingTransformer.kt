package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

@Suppress("UnusedPrivateMember")
internal class SetConfirmationStateLoadingTransformer(
    private val yield: Yield,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val possibleValidatorState = possibleConfirmationState?.validatorState as? ValidatorState.Content
        val chosenValidator = possibleValidatorState?.chosenValidator ?: yield.validators[0]

        return prevState.copy(
            confirmationState = StakingStates.ConfirmationState.Data(
                isPrimaryButtonEnabled = false,
                innerState = InnerConfirmationStakingState.ASSENT,
                feeState = FeeState.Loading,
                validatorState = ValidatorState.Content(
                    isClickable = false,
                    chosenValidator = chosenValidator,
                    availableValidators = yield.validators,
                ),
                notifications = persistentListOf(
                    StakingNotification.Warning.EarnRewards(
                        currencyName = yield.token.name,
                        days = yield.metadata.cooldownPeriod.days,
                    ),
                ),
                footerText = "",
                transactionDoneState = TransactionDoneState.Empty,
                pendingActions = persistentListOf(),
            ),
        )
    }
}
