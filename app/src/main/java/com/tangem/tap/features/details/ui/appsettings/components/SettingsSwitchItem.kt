package com.tangem.tap.features.details.ui.appsettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW32
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item
import com.tangem.tap.features.details.ui.common.TangemSwitch

@Composable
internal fun SettingsSwitchItem(item: Item.Switch, modifier: Modifier = Modifier) {
    val titleTextColor by rememberUpdatedState(
        newValue = if (item.isEnabled) {
            TangemTheme.colors.text.primary1
        } else {
            TangemTheme.colors.text.secondary
        },
    )
    val descriptionTextColor by rememberUpdatedState(
        newValue = if (item.isEnabled) {
            TangemTheme.colors.text.secondary
        } else {
            TangemTheme.colors.text.tertiary
        },
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(weight = .9f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = titleTextColor,
            )
            SpacerH4()
            Text(
                text = item.description.resolveReference(),
                style = TangemTheme.typography.body2,
                color = descriptionTextColor,
            )
        }
        SpacerW32()
        TangemSwitch(
            checked = item.isChecked,
            enabled = item.isEnabled,
            onCheckedChange = item.onCheckedChange,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SwitchItemPreview_Light(@PreviewParameter(SwitchItemProvider::class) item: Item.Switch) {
    TangemTheme {
        SettingsSwitchItem(item = item)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SwitchItemPreview_Dark(@PreviewParameter(SwitchItemProvider::class) item: Item.Switch) {
    TangemTheme(isDark = true) {
        SettingsSwitchItem(item = item)
    }
}

private class SwitchItemProvider : CollectionPreviewParameterProvider<Item.Switch>(
    collection = buildList {
        val itemsFactory = AppSettingsItemsFactory()

        itemsFactory.createSaveAccessCodeSwitch(
            isChecked = true,
            isEnabled = true,
            onCheckedChange = { /* no-op */ },
        ).let(::add)
        itemsFactory.createSaveAccessCodeSwitch(
            isChecked = false,
            isEnabled = true,
            onCheckedChange = { /* no-op */ },
        ).let(::add)
        itemsFactory.createSaveAccessCodeSwitch(
            isChecked = true,
            isEnabled = false,
            onCheckedChange = { /* no-op */ },
        ).let(::add)
        itemsFactory.createSaveAccessCodeSwitch(
            isChecked = false,
            isEnabled = false,
            onCheckedChange = { /* no-op */ },
        ).let(::add)
    },
)
// endregion Preview
