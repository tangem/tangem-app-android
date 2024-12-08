package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.AdditionalTextInputDialogUM
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.walletsettings.component.preview.PreviewRenameWalletComponent
import com.tangem.feature.walletsettings.entity.RenameWalletUM
import com.tangem.feature.walletsettings.impl.R

@Composable
internal fun RenameWalletDialog(model: RenameWalletUM, onDismiss: () -> Unit) {
    val value by rememberUpdatedState(newValue = model.walletNameValue)

    TextInputDialog(
        title = stringResourceSafe(id = R.string.user_wallet_list_rename_popup_title),
        fieldValue = value,
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_ok),
            enabled = model.isConfirmEnabled,
            onClick = model.onConfirm,
        ),
        dismissButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_cancel),
            onClick = onDismiss,
        ),
        onDismissDialog = onDismiss,
        onValueChange = model.updateValue,
        textFieldParams = AdditionalTextInputDialogUM(
            label = stringResourceSafe(id = R.string.user_wallet_list_rename_popup_placeholder),
        ),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_RenameWalletDialog() {
    TangemThemePreview {
        PreviewRenameWalletComponent().Dialog()
    }
}
// endregion Preview