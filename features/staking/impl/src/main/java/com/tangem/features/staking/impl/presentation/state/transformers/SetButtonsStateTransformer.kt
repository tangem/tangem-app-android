package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.getPendingActionTitle
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SetButtonsStateTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        val buttonsState = if (prevState.isButtonsVisible()) {
            NavigationButtonsState.Data(
                primaryButton = getPrimaryButton(prevState),
                prevButton = getPrevButton(prevState),
                extraButtons = getExtraButtons(prevState),
                txUrl = (confirmState?.transactionDoneState as? TransactionDoneState.Content)?.txUrl,
            )
        } else {
            NavigationButtonsState.Empty
        }

        return prevState.copy(buttonsState = buttonsState)
    }

    private fun getPrimaryButton(prevState: StakingUiState): NavigationButton {
        val confirmState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
        val innerConfirmState = confirmState?.innerState

        val isConfirmation = prevState.currentStep == StakingStep.Confirmation
        val isInProgress = innerConfirmState == InnerConfirmationStakingState.IN_PROGRESS
        val isCompleted = innerConfirmState == InnerConfirmationStakingState.COMPLETED

        val isIconVisible = isConfirmation && !isCompleted
        return NavigationButton(
            textReference = prevState.getButtonText(),
            iconRes = R.drawable.ic_tangem_24,
            isSecondary = false,
            isIconVisible = isIconVisible,
            showProgress = isInProgress,
            isEnabled = prevState.isButtonEnabled(),
            onClick = { prevState.onPrimaryClick() },
        )
    }

    private fun getPrevButton(prevState: StakingUiState): NavigationButton? {
        return NavigationButton(
            textReference = TextReference.EMPTY,
            iconRes = R.drawable.ic_back_24,
            isSecondary = true,
            isIconVisible = true,
            showProgress = false,
            isEnabled = true,
            onClick = prevState.clickIntents::onPrevClick,
        ).takeIf { prevState.currentStep.isPrevButtonVisible() }
    }

    private fun getExtraButtons(prevState: StakingUiState): ImmutableList<NavigationButton> {
        return persistentListOf(
            NavigationButton(
                textReference = resourceReference(R.string.common_explore),
                iconRes = R.drawable.ic_web_24,
                isSecondary = true,
                isIconVisible = true,
                showProgress = false,
                isEnabled = true,
                onClick = prevState.clickIntents::onExploreClick,
            ),
            NavigationButton(
                textReference = resourceReference(R.string.common_share),
                iconRes = R.drawable.ic_share_24,
                isSecondary = true,
                isIconVisible = true,
                showProgress = false,
                isEnabled = true,
                onClick = prevState.clickIntents::onShareClick,
            ),
        )
    }

    private fun StakingUiState.isButtonsVisible(): Boolean = when (currentStep) {
        StakingStep.RewardsValidators -> false
        else -> true
    }

    private fun StakingUiState.getButtonText(): TextReference {
        return when (currentStep) {
            StakingStep.InitialInfo -> {
                val initialState = initialInfoState as? StakingStates.InitialInfoState.Data
                if (initialState?.yieldBalance is InnerYieldBalanceState.Data) {
                    resourceReference(R.string.staking_stake_more)
                } else {
                    resourceReference(R.string.common_stake)
                }
            }

            StakingStep.Confirmation -> getConfirmationButtonText()
            StakingStep.Validators -> resourceReference(R.string.common_continue)
            StakingStep.Amount,
            StakingStep.RewardsValidators,
            -> resourceReference(R.string.common_next)
        }
    }

    private fun StakingUiState.getConfirmationButtonText(): TextReference {
        return if (confirmationState is StakingStates.ConfirmationState.Data) {
            if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                resourceReference(R.string.common_close)
            } else {
                when (actionType) {
                    StakingActionCommonType.ENTER -> {
                        if (confirmationState.isApprovalNeeded) {
                            resourceReference(R.string.give_permission_title)
                        } else {
                            resourceReference(R.string.common_stake)
                        }
                    }
                    StakingActionCommonType.EXIT -> resourceReference(R.string.common_unstake)
                    StakingActionCommonType.PENDING_OTHER,
                    StakingActionCommonType.PENDING_REWARDS,
                    -> confirmationState.pendingAction?.type.getPendingActionTitle()
                }
            }
        } else {
            resourceReference(R.string.common_close)
        }
    }

    private fun StakingUiState.onPrimaryClick() {
        when (currentStep) {
            StakingStep.InitialInfo -> {
                val actionType = StakingActionCommonType.ENTER
                clickIntents.onAmountValueChange("") // reset amount state
                clickIntents.onNextClick(actionType)
            }
            StakingStep.Validators,
            StakingStep.Amount,
            -> clickIntents.onNextClick()
            StakingStep.Confirmation -> onConfirmationClick()
            StakingStep.RewardsValidators -> Unit
        }
    }

    private fun StakingUiState.onConfirmationClick() {
        if (confirmationState is StakingStates.ConfirmationState.Data) {
            if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                clickIntents.onBackClick()
            } else {
                val isEnterAction = actionType == StakingActionCommonType.ENTER
                val isApproveNeeded = confirmationState.isApprovalNeeded

                if (isEnterAction && isApproveNeeded) {
                    clickIntents.showApprovalBottomSheet()
                } else {
                    clickIntents.onActionClick()
                }
            }
        } else {
            clickIntents.onBackClick()
        }
    }

    private fun StakingStep.isPrevButtonVisible(): Boolean = when (this) {
        StakingStep.InitialInfo,
        StakingStep.RewardsValidators,
        StakingStep.Confirmation,
        StakingStep.Validators,
        -> false
        StakingStep.Amount,
        -> true
    }

    private fun StakingUiState.isButtonEnabled(): Boolean {
        return when (currentStep) {
            StakingStep.InitialInfo -> initialInfoState.isPrimaryButtonEnabled
            StakingStep.Amount -> amountState.isPrimaryButtonEnabled
            StakingStep.Confirmation -> confirmationState.isPrimaryButtonEnabled
            StakingStep.RewardsValidators -> rewardsValidatorsState.isPrimaryButtonEnabled
            StakingStep.Validators -> true
        }
    }
}
