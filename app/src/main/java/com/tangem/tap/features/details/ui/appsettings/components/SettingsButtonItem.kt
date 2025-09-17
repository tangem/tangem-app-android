package com.tangem.tap.features.details.ui.appsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item

@Composable
internal fun SettingsButtonItem(item: Item.Button, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors.background.secondary,
        onClick = item.onClick,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TangemTheme.dimens.spacing20,
                vertical = TangemTheme.dimens.spacing8,
            ),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = item.title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = item.description.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ButtonItemPreview(@PreviewParameter(ButtonItemProvider::class) item: Item.Button) {
    TangemThemePreview {
        SettingsButtonItem(item = item)
    }
}

private class ButtonItemProvider : CollectionPreviewParameterProvider<Item.Button>(
    collection = buildList {
        val itemsFactory = AppSettingsItemsFactory()

        add(itemsFactory.createSelectAppCurrencyButton(
            currentAppCurrencyName = "US Dollar",
            onClick = { /* no-op */ },
        ))
    },
)
// endregion Preview