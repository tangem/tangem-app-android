package com.tangem.feature.walletsettings.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.entity.RenameWalletUM
import com.tangem.feature.walletsettings.ui.RenameWalletDialog

internal class PreviewRenameWalletComponent : RenameWalletComponent {

    private val previewState = RenameWalletUM(
        walletNameValue = TextFieldValue(text = "My Wallet"),
        isConfirmEnabled = false,
        onValueChange = {},
        onConfirmClick = {},
        error = null,
    )

    override fun dismiss() {}

    @Composable
    override fun Dialog() {
        RenameWalletDialog(model = previewState, onDismiss = {})
    }
}