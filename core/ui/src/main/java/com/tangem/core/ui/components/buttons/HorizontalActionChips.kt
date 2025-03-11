package com.tangem.core.ui.components.buttons

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HorizontalActionChips(
    buttons: ImmutableList<ActionButtonConfig>,
    modifier: Modifier = Modifier,
    containerColor: Color = TangemTheme.colors.background.secondary,
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
            itemContent = { ActionButton(config = it, containerColor = containerColor) },
        )
    }
}

// region Preview
@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_HorizontalActionChips(
    @PreviewParameter(ActionButtonConfigProvider::class) buttons: HorizontalActionChipsData,
) {
    TangemThemePreview {
        HorizontalActionChips(
            buttons = buttons.buttons,
            modifier = Modifier.padding(4.dp),
        )
    }
}

private class ActionButtonConfigProvider : CollectionPreviewParameterProvider<HorizontalActionChipsData>(
    collection = listOf(
        HorizontalActionChipsData(
            buttons = persistentListOf(
                ActionButtonConfig(
                    text = TextReference.Str(value = "Buy"),
                    iconResId = R.drawable.ic_plus_24,
                    onClick = {},
                ),
                ActionButtonConfig(
                    text = TextReference.Str(value = "Exchange"),
                    iconResId = R.drawable.ic_exchange_vertical_24,
                    onClick = {},
                    showBadge = true,
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
            ),
        ),
    ),
)

private data class HorizontalActionChipsData(
    val buttons: ImmutableList<ActionButtonConfig>,
)
// endregion Preview