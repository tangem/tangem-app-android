package com.tangem.tap.features.details.ui.appsettings.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.SelectorDialog
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.details.ui.appsettings.AppSettingsDialogsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState.Dialog
import com.tangem.wallet.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SettingsSelectorDialog(dialog: Dialog.Selector) {
    SelectorDialog(
        title = dialog.title.resolveReference(),
        selectedItemIndex = dialog.selectedItemIndex,
        items = dialog.items.map { it.resolveReference() }.toImmutableList(),
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(R.string.common_cancel),
            onClick = dialog.onDismiss,
        ),
        onSelect = dialog.onSelect,
        onDismissDialog = dialog.onDismiss,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsSelectorDialogPreview(@PreviewParameter(DialogProvider::class) param: Dialog.Selector) {
    TangemThemePreview {
        SettingsSelectorDialog(param)
    }
}

private class DialogProvider : CollectionPreviewParameterProvider<Dialog.Selector>(
    collection = listOf(
        AppSettingsDialogsFactory().createThemeModeSelectorDialog(
            selectedModeIndex = 0,
            onSelect = {},
            onDismiss = {},
        ),
    ),
)
// endregion Preview