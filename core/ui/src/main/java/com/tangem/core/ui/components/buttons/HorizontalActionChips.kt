package com.tangem.core.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HorizontalActionChips(
    buttons: ImmutableList<ActionButtonConfig>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = contentPadding,
    ) {
        items(
            items = buttons,
            key = { it.text },
            itemContent = { ActionButton(config = it) },
        )
    }
}

@Preview
@Composable
private fun Preview_HorizontalActionChips_Light(
    @PreviewParameter(ActionButtonConfigProvider::class) buttons: ImmutableList<ActionButtonConfig>,
) {
    TangemTheme(isDark = false) {
        HorizontalActionChips(buttons = buttons)
    }
}

@Preview
@Composable
private fun Preview_HorizontalActionChips_Dark(
    @PreviewParameter(ActionButtonConfigProvider::class) buttons: ImmutableList<ActionButtonConfig>,
) {
    TangemTheme(isDark = true) {
        HorizontalActionChips(buttons = buttons)
    }
}

private class ActionButtonConfigProvider : CollectionPreviewParameterProvider<ActionButtonConfig>(
    collection = persistentListOf(
        ActionButtonConfig(
            text = "Buy",
            iconResId = R.drawable.ic_plus_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = "Send",
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = "Receive",
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = {},
        ),
        ActionButtonConfig(
            text = "Exchange",
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = {},
        ),
    ),
)