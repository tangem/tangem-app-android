package com.tangem.core.ui.components.buttons

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
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HorizontalActionChips(
    buttons: ImmutableList<ActionButtonConfig>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(TangemTheme.dimens.spacing0),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = contentPadding,
    ) {
        // do not use key cause when change items order, list is scrolled
        items(
            items = buttons,
            itemContent = { ActionButton(config = it) },
        )
    }
}

// region Preview
@Preview(widthDp = 360)
@Composable
private fun Preview_HorizontalActionChips_Light(
    @PreviewParameter(ActionButtonConfigProvider::class) buttons: HorizontalActionChips,
) {
    TangemTheme(isDark = false) {
        HorizontalActionChips(buttons = buttons.buttons)
    }
}

@Preview(widthDp = 360)
@Composable
private fun Preview_HorizontalActionChips_Dark(
    @PreviewParameter(ActionButtonConfigProvider::class) buttons: HorizontalActionChips,
) {
    TangemTheme(isDark = true) {
        HorizontalActionChips(buttons = buttons.buttons)
    }
}

private class ActionButtonConfigProvider : CollectionPreviewParameterProvider<HorizontalActionChips>(
    collection = listOf(
        HorizontalActionChips(
            buttons = persistentListOf(
                ActionButtonConfig(
                    text = TextReference.Str(value = "Buy"),
                    iconResId = R.drawable.ic_plus_24,
                    onClick = {},
                ),
                ActionButtonConfig(
                    text = TextReference.Str(value = "Send"),
                    iconResId = R.drawable.ic_arrow_up_24,
                    onClick = {},
                ),
                ActionButtonConfig(
                    text = TextReference.Str(value = "Receive"),
                    iconResId = R.drawable.ic_arrow_down_24,
                    onClick = {},
                ),
                ActionButtonConfig(
                    text = TextReference.Str(value = "Exchange"),
                    iconResId = R.drawable.ic_exchange_vertical_24,
                    onClick = {},
                ),
            ),
        ),
    ),
)

private data class HorizontalActionChips(
    val buttons: ImmutableList<ActionButtonConfig>,
)
// endregion Preview
