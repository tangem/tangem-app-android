package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig

@Composable
internal fun TokenDetailsDialogs(state: TokenDetailsState) {
    val dialogConfig = state.dialogConfig
    if (dialogConfig != null && dialogConfig.isShow) {
        TokenDetailsDialog(config = dialogConfig)
    }
}

@Composable
private fun TokenDetailsDialog(config: TokenDetailsDialogConfig) {
    BasicDialog(
        message = config.content.message.resolveReference(),
        confirmButton = DialogButtonUM(
            title = config.content.confirmButtonConfig.text.resolveReference(),
            warning = config.content.confirmButtonConfig.warning,
            onClick = config.content.confirmButtonConfig.onClick,
        ),
        onDismissDialog = config.onDismissRequest,
        title = config.content.title?.resolveReference(),
        dismissButton = config.content.cancelButtonConfig?.let { cancelButtonConfig ->
            DialogButtonUM(
                title = cancelButtonConfig.text.resolveReference(),
                warning = cancelButtonConfig.warning,
                onClick = cancelButtonConfig.onClick,
            )
        },
    )
}