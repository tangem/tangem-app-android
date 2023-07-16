package com.tangem.core.ui.components.managebuttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HorizontalScrollableManageButtons(buttons: ImmutableList<ManageButtons>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            items = buttons,
            key = { it.config.text },
            itemContent = { ActionButton(config = it.config) },
        )
    }
}

@Preview
@Composable
private fun Preview_HorizontalScrollableManageButtons_Light(
    @PreviewParameter(ManageButtonProvider::class) buttons: ImmutableList<ManageButtons>,
) {
    TangemTheme(isDark = false) {
        HorizontalScrollableManageButtons(buttons = buttons)
    }
}

@Preview
@Composable
private fun Preview_HorizontalScrollableManageButtons_Dark(
    @PreviewParameter(ManageButtonProvider::class) buttons: ImmutableList<ManageButtons>,
) {
    TangemTheme(isDark = true) {
        HorizontalScrollableManageButtons(buttons = buttons)
    }
}

private class ManageButtonProvider : CollectionPreviewParameterProvider<ManageButtons>(
    collection = persistentListOf(
        ManageButtons.Buy(onClick = {}),
        ManageButtons.Send(onClick = {}),
        ManageButtons.Receive(onClick = {}),
        ManageButtons.Exchange(onClick = {}),
        ManageButtons.CopyAddress(onClick = {}),
    ),
)
