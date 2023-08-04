package com.tangem.core.ui.components.transactions.empty

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
        Image(
            modifier = Modifier.size(TangemTheme.dimens.size64),
            painter = painterResource(id = state.iconRes),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing32),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )

        ActionButton(config = state.actionButtonConfig)
    }
}

@Preview
@Composable
private fun EmptyTransactionBlock_Light(
    @PreviewParameter(EmptyTransactionBlockStateProvider::class) state: EmptyTransactionsBlockState,
) {
    TangemTheme {
        EmptyTransactionBlock(state = state)
    }
}

@Preview
@Composable
private fun EmptyTransactionBlock_Dark(
    @PreviewParameter(EmptyTransactionBlockStateProvider::class) state: EmptyTransactionsBlockState,
) {
    TangemTheme(isDark = true) {
        EmptyTransactionBlock(state = state)
    }
}

private class EmptyTransactionBlockStateProvider : CollectionPreviewParameterProvider<EmptyTransactionsBlockState>(
    collection = listOf(
        EmptyTransactionsBlockState.Empty(onClick = {}),
        EmptyTransactionsBlockState.FailedToLoad(onClick = {}),
        EmptyTransactionsBlockState.NotImplemented(onClick = {}),
    ),
)
