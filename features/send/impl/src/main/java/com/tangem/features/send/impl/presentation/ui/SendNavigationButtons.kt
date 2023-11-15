package com.tangem.features.send.impl.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType

@Composable
internal fun SendNavigationButtons(uiState: SendUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TangemTheme.dimens.spacing12),
    ) {
        SendSecondaryNavigationButton(uiState)
        SendPrimaryNavigationButton(
            uiState = uiState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun SendSecondaryNavigationButton(uiState: SendUiState) {
    val currentState = uiState.currentState.collectAsState()
    AnimatedVisibility(
        visible = currentState.value == SendUiStateType.Recipient ||
            currentState.value == SendUiStateType.Fee,
    ) {
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing16)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                .background(TangemTheme.colors.button.secondary)
                .clickable {
                    uiState.clickIntents.onPrevClick()
                }
                .padding(TangemTheme.dimens.spacing12),
            painter = painterResource(R.drawable.ic_back_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
    }
}

@Composable
private fun SendPrimaryNavigationButton(uiState: SendUiState, modifier: Modifier = Modifier) {
    val currentState = uiState.currentState.collectAsState()

    val buttonTextId = when (currentState.value) {
        SendUiStateType.Amount,
        SendUiStateType.Recipient,
        SendUiStateType.Fee,
        -> R.string.common_next
        SendUiStateType.Send -> R.string.common_send
        else -> R.string.common_close
    }

    val isButtonEnabled = when (currentState.value) {
        SendUiStateType.Amount -> uiState.amountState?.isPrimaryButtonEnabled ?: false
        SendUiStateType.Recipient -> uiState.recipientState?.isPrimaryButtonEnabled ?: false
        else -> true
    }

    AnimatedContent(
        targetState = buttonTextId,
        label = "Update send screen state",
        modifier = modifier,
    ) { textId ->
        if (currentState.value == SendUiStateType.Send) {
            PrimaryButtonIconEnd(
                text = stringResource(textId),
                iconResId = R.drawable.ic_tangem_24,
                enabled = isButtonEnabled,
                onClick = uiState.clickIntents::onNextClick,
            )
        } else {
            PrimaryButton(
                text = stringResource(textId),
                enabled = isButtonEnabled,
                onClick = uiState.clickIntents::onNextClick,
            )
        }
    }
}