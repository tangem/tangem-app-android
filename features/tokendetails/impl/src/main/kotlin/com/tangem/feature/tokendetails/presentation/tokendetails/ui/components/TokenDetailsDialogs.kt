package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.runtime.Composable
import com.tangem.common.ui.tokendetails.TokenDetailsDialogConfig
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference

@Composable
internal fun TokenDetailsDialogs(dialogConfig: TokenDetailsDialogConfig) {
    if (dialogConfig.isShow) {
        TokenDetailsDialog(config = dialogConfig)
    }
}

@Composable
private fun TokenDetailsDialog(config: TokenDetailsDialogConfig) {
    BasicDialog(
        message = config.content.message.resolveReference(),
        confirmButton = DialogButtonUM(
            title = config.content.confirmButtonConfig.text.resolveReference(),
            isWarning = config.content.confirmButtonConfig.hasWarning,
            onClick = config.content.confirmButtonConfig.onClick,
        ),
        onDismissDialog = config.onDismissRequest,
        title = config.content.title?.resolveReference(),
        dismissButton = config.content.cancelButtonConfig?.let { cancelButtonConfig ->
            DialogButtonUM(
                title = cancelButtonConfig.text.resolveReference(),
                isWarning = cancelButtonConfig.hasWarning,
                onClick = cancelButtonConfig.onClick,
            )
        },
    )
}