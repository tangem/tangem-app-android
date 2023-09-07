package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Item
import com.tangem.tap.features.details.ui.appsettings.components.*
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AppSettingsScreen(state: AppSettingsScreenState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            when (state) {
                is AppSettingsScreenState.Content -> AppSettings(state = state)
                is AppSettingsScreenState.Loading -> Unit
            }
        },
        titleRes = R.string.app_settings_title,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AppSettings(state: AppSettingsScreenState.Content) {
    val dialog by rememberUpdatedState(newValue = state.dialog)
    when (val safeDialog = dialog) {
        is AppSettingsScreenState.Dialog.Alert -> SettingsAlertDialog(dialog = safeDialog)
        is AppSettingsScreenState.Dialog.Selector -> SettingsSelectorDialog(dialog = safeDialog)
        null -> Unit
    }

    LazyColumn {
        items(
            items = state.items,
            key = Item::id,
        ) { item ->
            when (item) {
                is Item.Card -> SettingsCardItem(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                    item = item,
                )
                is Item.Button -> SettingsButtonItem(
                    modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8),
                    item = item,
                )
                is Item.Switch -> SettingsSwitchItem(
                    modifier = Modifier.padding(
                        vertical = TangemTheme.dimens.spacing16,
                        horizontal = TangemTheme.dimens.spacing20,
                    ),
                    item = item,
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreenPreview_Light(
    @PreviewParameter(AppSettingsScreenStateProvider::class) state: AppSettingsScreenState,
) {
    TangemTheme {
        AppSettingsScreen(state = state, onBackClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreenPreview_Dark(
    @PreviewParameter(AppSettingsScreenStateProvider::class) state: AppSettingsScreenState,
) {
    TangemTheme(isDark = true) {
        AppSettingsScreen(state = state, onBackClick = {})
    }
}

private class AppSettingsScreenStateProvider : CollectionPreviewParameterProvider<AppSettingsScreenState>(
    collection = buildList {
        val itemsFactory = AppSettingsItemsFactory()
        val items = persistentListOf(
            itemsFactory.createEnrollBiometricsCard {},
            itemsFactory.createSelectAppCurrencyButton(currentAppCurrencyName = "US Dollar") {},
            itemsFactory.createSaveWalletsSwitch(isChecked = true, isEnabled = true, { _ -> }),
            itemsFactory.createSaveAccessCodeSwitch(isChecked = false, isEnabled = true) { _ -> },
            itemsFactory.createSelectThemeModeButton(AppThemeMode.DEFAULT, {}),
        )

        AppSettingsScreenState.Content(
            items = items,
            dialog = null,
        ).let(::add)
    },
)
// endregion Preview
