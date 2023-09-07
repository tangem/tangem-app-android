package com.tangem.tap.features.details.ui.appsettings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW16
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SettingsCardItem(item: Item.Card, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors.button.disabled,
        shape = TangemTheme.shapes.roundedCornersLarge,
        onClick = item.onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Icon(
                painter = painterResource(id = item.iconResId),
                tint = TangemTheme.colors.icon.attention,
                contentDescription = null,
            )
            SpacerW16()
            Column {
                Text(
                    text = item.title.resolveReference(),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerH4()
                Text(
                    text = item.description.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CardItemPreview_Light(@PreviewParameter(CardItemProvider::class) item: Item.Card) {
    TangemTheme {
        SettingsCardItem(item = item)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CardItemPreview_Dark(@PreviewParameter(CardItemProvider::class) item: Item.Card) {
    TangemTheme(isDark = true) {
        SettingsCardItem(item = item)
    }
}

private class CardItemProvider : CollectionPreviewParameterProvider<Item.Card>(
    collection = buildList {
        val itemsFactory = AppSettingsItemsFactory()

        itemsFactory.createEnrollBiometricsCard(
            onClick = { /* no-op */ },
        ).let(::add)
    },
)
// endregion Preview
