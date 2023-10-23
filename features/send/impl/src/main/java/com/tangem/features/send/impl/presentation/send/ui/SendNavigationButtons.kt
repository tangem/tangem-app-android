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

@Composable
internal fun SendNavigationButtons(currentState: SendStates) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TangemTheme.dimens.spacing12),
    ) {
        SendSecondaryNavigationButton(currentState)
        SendPrimaryNavigationButton(
            currentState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun SendSecondaryNavigationButton(currentState: SendStates) {
    AnimatedVisibility(
        visible = currentState == SendStates.Recipient || currentState == SendStates.Fee,
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
private fun SendPrimaryNavigationButton(currentState: SendStates, modifier: Modifier = Modifier) {
    val buttonTextId = when (currentState) {
        SendStates.Amount,
        SendStates.Recipient,
        SendStates.Fee,
        -> R.string.common_next
        SendStates.Filled -> R.string.common_send
        SendStates.Done -> R.string.common_close
    }
    AnimatedContent(
        targetState = buttonTextId,
        label = "Update send screen state",
        modifier = modifier,
    ) { textId ->
        when (currentState) {
            SendStates.Amount,
            SendStates.Recipient,
            SendStates.Fee,
            SendStates.Done,
            -> PrimaryButton(
                text = stringResource(textId),
                onClick = {
                    // todo add next click
                },
            )
            SendStates.Filled -> {
                PrimaryButtonIconEnd(
                    text = stringResource(textId),
                    iconResId = R.drawable.ic_tangem_24,
                    onClick = {
                        // todo add next click
                    },
                )
            }
        }
    }
}