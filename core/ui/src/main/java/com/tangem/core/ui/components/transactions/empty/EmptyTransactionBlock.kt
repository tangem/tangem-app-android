package com.tangem.core.ui.components.transactions.empty

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Placeholder for transaction's block without content
 *
 * @param state    component state
 * @param modifier modifier
 */
@Composable
fun EmptyTransactionBlock(state: EmptyTransactionsBlockState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.primary)
            .padding(vertical = TangemTheme.dimens.spacing24),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size64),
            painter = painterResource(id = state.iconRes),
            tint = TangemTheme.colors.icon.inactive,
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing32),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )

        Buttons(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing18),
            state = state.buttonsState,
        )
    }
}

@Composable
private fun Buttons(state: EmptyTransactionsBlockState.ButtonsState, modifier: Modifier = Modifier) {
    when (state) {
        is EmptyTransactionsBlockState.ButtonsState.SingleButton -> SingleButton(state = state, modifier = modifier)
        is EmptyTransactionsBlockState.ButtonsState.PairButtons -> PairButtons(state = state, modifier = modifier)
    }
}

@Composable
private fun SingleButton(state: EmptyTransactionsBlockState.ButtonsState.SingleButton, modifier: Modifier = Modifier) {
    ActionButton(modifier = modifier, config = state.actionButtonConfig)
}

@Composable
private fun PairButtons(state: EmptyTransactionsBlockState.ButtonsState.PairButtons, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        ActionButton(
            modifier = Modifier.weight(1F),
            config = state.firstButtonConfig,
        )
        ActionButton(
            modifier = Modifier.weight(1F),
            config = state.secondButtonConfig,
        )
    }
}

@Composable
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EmptyTransactionBlockPreview(
    @PreviewParameter(EmptyTransactionBlockStateProvider::class) state: EmptyTransactionsBlockState,
) {
    TangemThemePreview {
        EmptyTransactionBlock(state = state)
    }
}

private class EmptyTransactionBlockStateProvider : CollectionPreviewParameterProvider<EmptyTransactionsBlockState>(
    collection = listOf(
        EmptyTransactionsBlockState.Empty {},
        EmptyTransactionsBlockState.FailedToLoad(onReload = {}, onExplore = {}),
        EmptyTransactionsBlockState.NotImplemented(onExplore = {}),
    ),
)