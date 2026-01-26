package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class ValidatorSelectChangeTransformer(
    private val integration: StakingIntegration,
    private val selectedTarget: StakingTarget?,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val validatorState = prevState.validatorState as? StakingStates.ValidatorState.Data
        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        val isRestake = prevState.actionType == StakingActionCommonType.Pending.Restake ||
            prevState.actionType is StakingActionCommonType.Pending.Stake
        val isEnter = prevState.actionType is StakingActionCommonType.Enter
        val isFromInfoScreen = prevState.currentStep == StakingStep.InitialInfo
        val isVoteLocked = confirmationState?.pendingAction?.type == StakingActionType.VOTE_LOCKED

        val activeTarget = selectedTarget.takeIf { isFromInfoScreen && isRestake }
            ?: validatorState?.activeTarget
        val filteredTargets = integration.preferredTargets.filterNot { it == activeTarget }

        val selectedTarget = if (isRestake && isFromInfoScreen) {
            filteredTargets.firstOrNull()
        } else {
            selectedTarget
        }

        if (selectedTarget == null && integration.preferredTargets.isEmpty()) {
            return prevState
        }

        return prevState.copy(
            validatorState = StakingStates.ValidatorState.Data(
                chosenTarget = selectedTarget ?: integration.preferredTargets.first(),
                availableTargets = filteredTargets,
                isPrimaryButtonEnabled = true,
                isClickable = integration.preferredTargets.size > 1,
                activeTarget = activeTarget,
                isVisibleOnConfirmation = isEnter || isRestake || isVoteLocked,
            ),
        )
    }
}