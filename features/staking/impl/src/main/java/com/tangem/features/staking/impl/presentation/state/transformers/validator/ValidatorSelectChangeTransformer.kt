package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class ValidatorSelectChangeTransformer(
    private val yield: Yield,
    private val selectedValidator: Yield.Validator?,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val validatorState = prevState.validatorState as? StakingStates.ValidatorState.Data
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        val isRestake = prevState.actionType == StakingActionCommonType.Pending.Restake ||
            prevState.actionType is StakingActionCommonType.Pending.Stake
        val isEnter = prevState.actionType is StakingActionCommonType.Enter
        val isFromInfoScreen = prevState.currentStep == StakingStep.InitialInfo
        val isVoteLocked = confirmationState?.pendingAction?.type == StakingActionType.VOTE_LOCKED

        val activeValidator = selectedValidator.takeIf { isFromInfoScreen && isRestake }
            ?: validatorState?.activeValidator
        val filteredValidators = yield.preferredValidators.filterNot { it == activeValidator }

        val selectedValidator = if (isRestake && isFromInfoScreen) {
            filteredValidators.firstOrNull()
        } else {
            selectedValidator
        }

        return prevState.copy(
            validatorState = StakingStates.ValidatorState.Data(
                chosenValidator = selectedValidator ?: yield.preferredValidators.first(),
                availableValidators = filteredValidators,
                isPrimaryButtonEnabled = true,
                isClickable = true,
                activeValidator = activeValidator,
                isVisibleOnConfirmation = isEnter || isRestake || isVoteLocked,
            ),
        )
    }
}