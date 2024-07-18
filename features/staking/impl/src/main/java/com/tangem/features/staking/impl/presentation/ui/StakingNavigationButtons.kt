package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.common.ui.amountScreen.ui.SendDoneButtons
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState

@Composable
internal fun StakingNavigationButtons(uiState: StakingUiState, modifier: Modifier = Modifier) {
    val confirmInnerState = (uiState.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState
    val isSuccessState = confirmInnerState == InnerConfirmationStakingState.COMPLETED

    Column(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        val confirmationDataState = uiState.confirmationState as? StakingStates.ConfirmationState.Data
        val transactionDoneState = confirmationDataState?.transactionDoneState as? TransactionDoneState.Content

        SendDoneButtons(
            txUrl = transactionDoneState?.txUrl.orEmpty(),
            onExploreClick = uiState.clickIntents::onExploreClick,
            onShareClick = uiState.clickIntents::onShareClick,
            isVisible = isSuccessState,
        )
        StakingNavigationButton(
            uiState = uiState,
            modifier = Modifier,
        )
    }
}

@Composable
private fun StakingNavigationButton(uiState: StakingUiState, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current

    val isButtonsVisible = isPrevButtonVisible(uiState.currentStep)

    val innerConfirmState = (uiState.confirmationState as? StakingStates.ConfirmationState.Data)?.innerState
    val isInProgressInnerState = innerConfirmState == InnerConfirmationStakingState.IN_PROGRESS
    val isInAssentInnerState = innerConfirmState == InnerConfirmationStakingState.ASSENT

    val showTangemIcon = uiState.currentStep == StakingStep.Confirmation &&
        (isInProgressInnerState || isInAssentInnerState)

    val (buttonTextId, buttonClick) = getButtonData(
        currentState = uiState,
    )
    val isButtonEnabled = isButtonEnabled(uiState)
    val buttonIcon = if (showTangemIcon) {
        TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
    } else {
        TangemButtonIconPosition.None
    }

    Row(modifier = modifier) {
        AnimatedVisibility(
            visible = isButtonsVisible,
            enter = expandHorizontally(expandFrom = Alignment.End),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                        .background(TangemTheme.colors.button.secondary)
                        .clickable { uiState.clickIntents.onPrevClick() }
                        .padding(TangemTheme.dimens.spacing12),
                )
                SpacerW12()
            }
        }
        AnimatedVisibility(
            visible = buttonClick != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TangemButton(
                text = stringResource(buttonTextId),
                icon = buttonIcon,
                enabled = isButtonEnabled && buttonClick != null,
                onClick = {
                    if (showTangemIcon) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (buttonClick != null) buttonClick()
                },
                showProgress = isInProgressInnerState,
                modifier = Modifier.fillMaxWidth(),
                colors = TangemButtonsDefaults.primaryButtonColors,
            )
        }
    }
}

private fun getButtonData(currentState: StakingUiState): Pair<Int, (() -> Unit)?> {
    return when (currentState.currentStep) {
        StakingStep.InitialInfo -> {
            val initialState = currentState.initialInfoState as? StakingStates.InitialInfoState.Data
            if (initialState?.yieldBalance is InnerYieldBalanceState.Data) {
                val click = if (initialState.isStakeMoreAvailable) {
                    currentState.clickIntents::onNextClick
                } else {
                    null
                }
                R.string.staking_stake_more to click
            } else {
                R.string.common_next to currentState.clickIntents::onNextClick
            }
        }
        StakingStep.Amount,
        -> R.string.common_next to currentState.clickIntents::onNextClick
        StakingStep.Confirmation -> {
            val confirmationState = currentState.confirmationState
            if (confirmationState is StakingStates.ConfirmationState.Data) {
                if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                    R.string.common_close to currentState.clickIntents::onBackClick
                } else {
                    R.string.common_stake to currentState.clickIntents::onNextClick
                }
            } else {
                R.string.common_close to currentState.clickIntents::onBackClick
            }
        }
        StakingStep.Validators -> R.string.common_continue to currentState.clickIntents::onNextClick
        StakingStep.RewardsValidators -> R.string.common_next to null
    }
}

private fun isPrevButtonVisible(step: StakingStep): Boolean = when (step) {
    StakingStep.InitialInfo,
    StakingStep.RewardsValidators,
    StakingStep.Confirmation,
    -> false
    StakingStep.Amount,
    StakingStep.Validators,
    -> true
}

private fun isButtonEnabled(uiState: StakingUiState): Boolean {
    return when (uiState.currentStep) {
        StakingStep.InitialInfo -> uiState.initialInfoState.isPrimaryButtonEnabled
        StakingStep.Amount -> uiState.amountState.isPrimaryButtonEnabled
        StakingStep.Confirmation -> uiState.confirmationState.isPrimaryButtonEnabled
        StakingStep.RewardsValidators -> uiState.rewardsValidatorsState.isPrimaryButtonEnabled
        StakingStep.Validators -> true
    }
}