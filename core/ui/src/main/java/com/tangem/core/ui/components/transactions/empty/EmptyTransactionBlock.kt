package com.tangem.core.ui.components.transactions.empty

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.EmptyTransactionBlockTestTags

@Composable
fun EmptyTransactionBlock(state: EmptyTransactionsBlockState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x6)
            .testTag(EmptyTransactionBlockTestTags.BLOCK),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Icon(
                iconRes = state.iconRes,
                tintReference = { TangemTheme.colors2.graphic.neutral.tertiary },
            ),
            modifier = Modifier
                .size(TangemTheme.dimens2.x16)
                .testTag(EmptyTransactionBlockTestTags.ICON),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x4))

        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens2.x8)
                .testTag(EmptyTransactionBlockTestTags.TEXT),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography2.calloutRegular15,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens2.x8))

        Buttons(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens2.x4)
                .testTag(EmptyTransactionBlockTestTags.EXPLORE_BUTTON),
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        TangemButton(buttonUM = state.actionButtonConfig.toButtonUM())
    }
}

@Composable
private fun PairButtons(state: EmptyTransactionsBlockState.ButtonsState.PairButtons, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        TangemButton(
            modifier = Modifier.weight(1F),
            buttonUM = state.firstButtonConfig.toButtonUM(),
        )
        TangemButton(
            modifier = Modifier.weight(1F),
            buttonUM = state.secondButtonConfig.toButtonUM(),
        )
    }
}

private fun ActionButtonConfig.toButtonUM(): TangemButtonUM = TangemButtonUM(
    text = text,
    tangemIconUM = TangemIconUM.Icon(iconRes = iconResId),
    type = TangemButtonType.Secondary,
    size = TangemButtonSize.X12,
    isEnabled = isEnabled,
    isLoading = isInProgress,
    onClick = onClick,
    shape = TangemButtonShape.Rounded,
    iconPosition = TangemButtonIconPosition.End,
)

@Composable
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EmptyTransactionBlockPreview(
    @PreviewParameter(EmptyTransactionBlockStateProvider::class) state: EmptyTransactionsBlockState,
) {
    TangemThemePreviewRedesign {
        EmptyTransactionBlock(state = state)
    }
}

private class EmptyTransactionBlockStateProvider :
    CollectionPreviewParameterProvider<EmptyTransactionsBlockState>(
        collection = listOf(
            EmptyTransactionsBlockState.Empty(
                onExplore = {},
                exploreIconResId = R.drawable.ic_compass_24,
            ),
            EmptyTransactionsBlockState.FailedToLoad(
                onReload = {},
                onExplore = {},
                reloadIconResId = R.drawable.ic_refresh_24,
                exploreIconResId = R.drawable.ic_compass_24,
            ),
            EmptyTransactionsBlockState.NotImplemented(
                onExplore = {},
                exploreIconResId = R.drawable.ic_compass_24,
            ),
        ),
    )