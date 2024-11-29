package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
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

        val isRestake = prevState.actionType == StakingActionCommonType.Pending.Restake
        val isEnter = prevState.actionType == StakingActionCommonType.Enter
        val isFromInfoScreen = prevState.currentStep == StakingStep.InitialInfo
        val canChooseValidator = confirmationState?.pendingAction?.type?.canChooseValidator ?: false

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
                isVisibleOnConfirmation = isEnter || isRestake || canChooseValidator,
            ),
        )
    }
}