package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.getPendingActionTitle
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SetButtonsStateTransformer(
    private val urlOpener: UrlOpener,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        val buttonsState = if (prevState.isButtonsVisible()) {
            NavigationButtonsState.Data(
                primaryButton = getPrimaryButton(prevState),
                prevButton = getPrevButton(prevState),
                extraButtons = getExtraButtons(prevState),
                txUrl = (confirmState?.transactionDoneState as? TransactionDoneState.Content)?.txUrl,
                onTextClick = urlOpener::openUrl,
            )
        } else {
            NavigationButtonsState.Empty
        }

        return prevState.copy(buttonsState = buttonsState)
    }

    private fun getPrimaryButton(prevState: StakingUiState): NavigationButton? {
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
        ).takeIf { prevState.isPrimaryButtonVisible() }
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
            StakingStep.RestakeValidator,
            StakingStep.RewardsValidators,
            -> resourceReference(R.string.common_next)
        }
    }

    private fun StakingUiState.getConfirmationButtonText(): TextReference {
        val confirmationState = confirmationState as? StakingStates.ConfirmationState.Data
        val amountState = amountState as? AmountState.Data
        return if (confirmationState != null && amountState != null) {
            if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                resourceReference(R.string.common_close)
            } else {
                when (actionType) {
                    is StakingActionCommonType.Enter -> {
                        val amount = amountState.amountTextField.cryptoAmount.value.orZero()
                        if (confirmationState.isApprovalNeeded && confirmationState.allowance < amount) {
                            resourceReference(R.string.give_permission_title)
                        } else {
                            resourceReference(R.string.common_stake)
                        }
                    }
                    is StakingActionCommonType.Exit -> resourceReference(R.string.common_unstake)
                    is StakingActionCommonType.Pending -> confirmationState.pendingAction?.type.getPendingActionTitle()
                }
            }
        } else {
            resourceReference(R.string.common_close)
        }
    }

    private fun StakingUiState.onPrimaryClick() {
        when (currentStep) {
            StakingStep.InitialInfo -> clickIntents.onNextClick()
            StakingStep.Validators,
            StakingStep.RestakeValidator,
            -> clickIntents.onNextClick()
            StakingStep.Amount -> clickIntents.onAmountEnterClick()
            StakingStep.Confirmation -> onConfirmationClick()
            StakingStep.RewardsValidators -> Unit
        }
    }

    private fun StakingUiState.onConfirmationClick() {
        val confirmationState = confirmationState as? StakingStates.ConfirmationState.Data
        val amountState = amountState as? AmountState.Data
        if (confirmationState != null && amountState != null) {
            if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                clickIntents.onNextClick()
            } else {
                val amount = amountState.amountTextField.cryptoAmount.value.orZero()
                val isEnterAction = actionType is StakingActionCommonType.Enter
                if (isEnterAction && confirmationState.isApprovalNeeded && confirmationState.allowance < amount) {
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
        StakingStep.RestakeValidator,
        StakingStep.Confirmation,
        StakingStep.Validators,
        -> false
        StakingStep.Amount,
        -> true
    }

    private fun StakingUiState.isPrimaryButtonVisible(): Boolean {
        val initialState = initialInfoState as? StakingStates.InitialInfoState.Data
        val hasNotStaking = initialState?.yieldBalance == InnerYieldBalanceState.Empty
        val isCardano = BlockchainUtils.isCardano(cryptoCurrencyBlockchainId)

        return hasNotStaking || !(isCardano && currentStep == StakingStep.InitialInfo)
    }

    private fun StakingUiState.isButtonEnabled(): Boolean {
        return when (currentStep) {
            StakingStep.InitialInfo -> initialInfoState.isPrimaryButtonEnabled
            StakingStep.Amount -> amountState.isPrimaryButtonEnabled
            StakingStep.Confirmation -> confirmationState.isPrimaryButtonEnabled
            StakingStep.RewardsValidators -> rewardsValidatorsState.isPrimaryButtonEnabled
            StakingStep.RestakeValidator,
            StakingStep.Validators,
            -> true
        }
    }
}