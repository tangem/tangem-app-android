package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.Yield
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

@Suppress("UnusedPrivateMember")
internal class SetConfirmLoadingStateTransformer(
    private val yield: Yield,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmStakingState = prevState.confirmStakingState as? StakingStates.ConfirmStakingState.Data
        val possibleValidatorState = possibleConfirmStakingState?.validatorState as? ValidatorState.Content
        val chosenValidator = possibleValidatorState?.chosenValidator ?: yield.validators[0] // TODO staking add sorting

        return prevState.copy(
            confirmStakingState = StakingStates.ConfirmStakingState.Data(
                isPrimaryButtonEnabled = false,
                feeState = FeeState.Loading,
                validatorState = ValidatorState.Content(
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
                isSuccess = false,
                isStaking = false, // TODO staking move to enum
            ),
        )
    }
}