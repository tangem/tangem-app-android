package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.getPendingActionTitle
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.Transformer
import kotlin.text.isNullOrEmpty

internal class SetButtonsStateTransformer(
    private val urlOpener: UrlOpener,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val confirmState = prevState.confirmationState as? StakingStates.ConfirmationState.Data

        val txUrl = (confirmState?.transactionDoneState as? TransactionDoneState.Content)?.txUrl
        val buttonsState = if (prevState.isButtonsVisible()) {
            NavigationButtonsState.Data(
                primaryButton = getPrimaryButton(prevState),
                extraButtons = getExtraButtons(prevState).takeUnless { txUrl.isNullOrEmpty() },
                txUrl = txUrl,
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

        val isHoldToConfirm = prevState.shouldShowHoldToConfirmButton && isConfirmation && !isCompleted
        val isIconVisible = isConfirmation && !isCompleted && !isHoldToConfirm
        val isPrimaryButtonDisabled = prevState.isPrimaryButtonDisabled()
        return NavigationButton(
            textReference = prevState.getButtonText(isHoldToConfirm),
            iconRes = R.drawable.ic_tangem_24.takeIf { prevState.showColdWalletInteractionIcon },
            isDimmed = isPrimaryButtonDisabled,
            isIconVisible = isIconVisible,
            shouldShowProgress = isInProgress,
            isEnabled = prevState.isButtonEnabled(),
            isHoldToConfirm = isHoldToConfirm,
            onClick = {
                if (isPrimaryButtonDisabled) {
                    prevState.clickIntents.showPrimaryClickAlert()
                } else {
                    prevState.onPrimaryClick()
                }
            },
        )
    }

    private fun getExtraButtons(prevState: StakingUiState): Pair<NavigationButton, NavigationButton> {
        return NavigationButton(
            textReference = resourceReference(R.string.common_explore),
            iconRes = R.drawable.ic_web_24,
            isSecondary = true,
            isIconVisible = true,
            shouldShowProgress = false,
            isEnabled = true,
            onClick = prevState.clickIntents::onExploreClick,
        ) to NavigationButton(
            textReference = resourceReference(R.string.common_share),
            iconRes = R.drawable.ic_share_24,
            isSecondary = true,
            isIconVisible = true,
            shouldShowProgress = false,
            isEnabled = true,
            onClick = prevState.clickIntents::onShareClick,
        )
    }

    private fun StakingUiState.isButtonsVisible(): Boolean = when (currentStep) {
        StakingStep.RewardsValidators -> false
        else -> true
    }

    private fun StakingUiState.getButtonText(isHoldToConfirm: Boolean): TextReference {
        return when (currentStep) {
            StakingStep.InitialInfo -> {
                val initialState = initialInfoState as? StakingStates.InitialInfoState.Data
                if (initialState?.yieldBalance is InnerYieldBalanceState.Data) {
                    resourceReference(R.string.staking_stake_more)
                } else {
                    resourceReference(R.string.common_stake)
                }
            }
            StakingStep.Success -> resourceReference(R.string.common_close)
            StakingStep.Confirmation -> getConfirmationButtonText(isHoldToConfirm)
            StakingStep.Validators -> resourceReference(R.string.common_continue)
            StakingStep.Amount,
            StakingStep.RestakeValidator,
            StakingStep.RewardsValidators,
            -> resourceReference(R.string.common_next)
        }
    }

    private fun StakingUiState.getConfirmationButtonText(isHoldToConfirm: Boolean): TextReference {
        val confirmationState = confirmationState as? StakingStates.ConfirmationState.Data
            ?: return resourceReference(R.string.common_close)
        val amountState = amountState as? AmountState.Data
            ?: return resourceReference(R.string.common_close)

        if (actionType is StakingActionCommonType.Enter) {
            val amount = amountState.amountTextField.cryptoAmount.value.orZero()
            if (confirmationState.isApprovalNeeded && confirmationState.allowance < amount) {
                return resourceReference(R.string.give_permission_title)
            }
        }

        val baseText = getBaseActionText(confirmationState)
            ?: return resourceReference(R.string.common_close)

        return baseText.wrapWithHoldToIf(isHoldToConfirm)
    }

    private fun StakingUiState.getBaseActionText(
        confirmationState: StakingStates.ConfirmationState.Data,
    ): TextReference? = when (actionType) {
        is StakingActionCommonType.Enter -> resourceReference(R.string.common_stake)
        is StakingActionCommonType.Exit -> resourceReference(R.string.common_unstake)
        is StakingActionCommonType.Pending -> confirmationState.pendingAction?.type.getPendingActionTitle()
    }

    private fun TextReference.wrapWithHoldToIf(condition: Boolean): TextReference = if (condition) {
        resourceReference(id = R.string.common_hold_to, formatArgs = wrappedList(this))
    } else {
        this
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
            StakingStep.Success -> clickIntents.onNextClick()
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

    private fun StakingUiState.isPrimaryButtonDisabled(): Boolean {
        val initialState = initialInfoState as? StakingStates.InitialInfoState.Data
        val hasNotStaking = initialState?.yieldBalance == InnerYieldBalanceState.Empty
        val isCardano = BlockchainUtils.isCardano(cryptoCurrencyBlockchainId)

        return !hasNotStaking && isCardano && currentStep == StakingStep.InitialInfo
    }

    private fun StakingUiState.isButtonEnabled(): Boolean {
        return when (currentStep) {
            StakingStep.InitialInfo -> initialInfoState.isPrimaryButtonEnabled
            StakingStep.Amount -> amountState.isPrimaryButtonEnabled
            StakingStep.Confirmation -> confirmationState.isPrimaryButtonEnabled
            StakingStep.RewardsValidators -> rewardsValidatorsState.isPrimaryButtonEnabled
            StakingStep.RestakeValidator,
            StakingStep.Validators,
            StakingStep.Success,
            -> true
        }
    }
}