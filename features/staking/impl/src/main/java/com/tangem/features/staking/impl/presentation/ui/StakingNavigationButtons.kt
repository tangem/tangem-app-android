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

    val buttonTextId = getButtonData(currentState = uiState)
    val (isButtonEnabled, isButtonDisplayed) = isButtonEnabled(uiState)
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
            visible = isButtonDisplayed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TangemButton(
                text = stringResource(buttonTextId),
                icon = buttonIcon,
                enabled = isButtonEnabled && isButtonDisplayed,
                onClick = {
                    if (showTangemIcon) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPrimaryClick(uiState)
                },
                showProgress = isInProgressInnerState,
                modifier = Modifier.fillMaxWidth(),
                colors = TangemButtonsDefaults.primaryButtonColors,
            )
        }
    }
}

private fun getButtonData(currentState: StakingUiState): Int {
    return when (currentState.currentStep) {
        StakingStep.InitialInfo -> {
            val initialState = currentState.initialInfoState as? StakingStates.InitialInfoState.Data
            if (initialState?.yieldBalance is InnerYieldBalanceState.Data) {
                R.string.staking_stake_more
            } else {
                R.string.common_next
            }
        }
        StakingStep.Confirmation -> {
            val confirmationState = currentState.confirmationState
            if (confirmationState is StakingStates.ConfirmationState.Data) {
                if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                    R.string.common_close
                } else {
                    when (currentState.routeType) {
                        RouteType.STAKE -> R.string.common_stake
                        RouteType.CLAIM -> R.string.common_claim_rewards
                        RouteType.UNSTAKE -> R.string.common_unstake
                        RouteType.OTHER -> R.string.common_stake
                    }
                }
            } else {
                R.string.common_close
            }
        }
        StakingStep.Validators -> R.string.common_continue
        StakingStep.Amount,
        StakingStep.RewardsValidators,
        -> R.string.common_next
    }
}

private fun onPrimaryClick(currentState: StakingUiState) {
    when (currentState.currentStep) {
        StakingStep.InitialInfo,
        StakingStep.Validators,
        StakingStep.Amount,
        -> currentState.clickIntents.onNextClick()
        StakingStep.Confirmation -> {
            val confirmationState = currentState.confirmationState
            if (confirmationState is StakingStates.ConfirmationState.Data) {
                if (confirmationState.innerState == InnerConfirmationStakingState.COMPLETED) {
                    currentState.clickIntents.onBackClick()
                } else {
                    currentState.clickIntents.onNextClick()
                }
            } else {
                currentState.clickIntents.onBackClick()
            }
        }
        StakingStep.RewardsValidators -> Unit
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

private fun isButtonEnabled(uiState: StakingUiState): Pair<Boolean, Boolean> {
    return when (uiState.currentStep) {
        StakingStep.InitialInfo -> {
            val initialState = uiState.initialInfoState as? StakingStates.InitialInfoState.Data
            val isDisplayed = initialState?.isStakeMoreAvailable == true ||
                initialState?.yieldBalance is InnerYieldBalanceState.Empty
            uiState.initialInfoState.isPrimaryButtonEnabled to isDisplayed
        }
        StakingStep.Amount -> uiState.amountState.isPrimaryButtonEnabled to true
        StakingStep.Confirmation -> uiState.confirmationState.isPrimaryButtonEnabled to true
        StakingStep.RewardsValidators -> uiState.rewardsValidatorsState.isPrimaryButtonEnabled to false
        StakingStep.Validators -> true to true
    }
}