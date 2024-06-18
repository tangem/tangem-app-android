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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.StakingStep

@Composable
internal fun StakingNavigationButtons(uiState: StakingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        StakingNavigationButton(
            uiState = uiState,
            modifier = Modifier,
        )
    }
}

@Composable
private fun StakingNavigationButton(uiState: StakingUiState, modifier: Modifier = Modifier) {
    val hapticFeedback = LocalHapticFeedback.current
    val confirmState = uiState.confirmStakingState
    val isSuccess = (confirmState as? StakingStates.ConfirmStakingState.Data)?.isSuccess ?: false
    val isStaking = (confirmState as? StakingStates.ConfirmStakingState.Data)?.isStaking ?: false

    val isButtonsVisible = uiState.currentStep != StakingStep.Success
    val isStakingState = uiState.currentStep == StakingStep.Success && !isSuccess && !isStaking

    val (buttonTextId, buttonClick) = getButtonData(
        currentState = uiState,
    )
    val isButtonEnabled = isButtonEnabled(uiState)
    val buttonIcon = if (isStakingState) {
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
        TangemButton(
            text = stringResource(buttonTextId),
            icon = buttonIcon,
            enabled = isButtonEnabled,
            onClick = {
                if (isStakingState) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                buttonClick()
            },
            showProgress = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TangemButtonsDefaults.primaryButtonColors,
        )
    }
}

private fun getButtonData(currentState: StakingUiState): Pair<Int, () -> Unit> {
    return when (currentState.currentStep) {
        StakingStep.InitialInfo,
        StakingStep.Amount,
        StakingStep.Confirm,
        StakingStep.Success,
        -> R.string.common_next to { currentState.clickIntents.onNextClick() }
    }
}

private fun isButtonEnabled(uiState: StakingUiState): Boolean {
    return when (uiState.currentStep) {
        StakingStep.InitialInfo -> uiState.initialInfoState.isPrimaryButtonEnabled
        StakingStep.Amount -> uiState.amountState.isPrimaryButtonEnabled
        StakingStep.Confirm -> uiState.confirmStakingState.isPrimaryButtonEnabled
        StakingStep.Success -> true
    }
}