package com.tangem.features.send.impl.presentation.send.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.send.state.SendUiState

@Composable
internal fun SendNavigationButtons(uiState: SendUiState.Content) {
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
private fun SendSecondaryNavigationButton(uiState: SendUiState.Content) {
    AnimatedVisibility(
        visible = uiState is SendUiState.Content.RecipientState || uiState is SendUiState.Content.FeeState,
    ) {
        Icon(
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing16)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius16))
                .background(TangemTheme.colors.button.secondary)
                .clickable {
                    // todo add prev click
                }
                .padding(TangemTheme.dimens.spacing12),
            painter = painterResource(R.drawable.ic_back_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun SendPrimaryNavigationButton(uiState: SendUiState.Content, modifier: Modifier = Modifier) {
    val buttonTextId = when (uiState) {
        is SendUiState.Content.AmountState,
        is SendUiState.Content.RecipientState,
        is SendUiState.Content.FeeState,
        -> R.string.common_next
        is SendUiState.Content.SendState -> R.string.common_send
        else -> R.string.common_close
    }
    AnimatedContent(
        targetState = buttonTextId,
        label = "Update send screen state",
        modifier = modifier,
    ) { textId ->
        if (uiState is SendUiState.Content.SendState) {
            PrimaryButtonIconEnd(
                text = stringResource(textId),
                iconResId = R.drawable.ic_tangem_24,
                enabled = uiState.isPrimaryButtonEnabled,
                onClick = {
                    // todo add next click
                },
            )
        } else {
            PrimaryButton(
                text = stringResource(textId),
                enabled = uiState.isPrimaryButtonEnabled,
                onClick = {
                    // todo add next click
                },
            )
        }
    }
}