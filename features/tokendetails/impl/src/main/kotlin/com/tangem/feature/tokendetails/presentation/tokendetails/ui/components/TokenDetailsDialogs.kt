package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
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
    AlertDialog(
        onDismissRequest = config.onDismissRequest,
        containerColor = TangemTheme.colors.background.primary,
        title = {
            Text(
                text = config.content.title.resolveReference(),
                style = TangemTheme.typography.h2,
            )
        },
        titleContentColor = TangemTheme.colors.text.primary1,
        text = {
            Text(
                text = config.content.text.resolveReference(),
                style = TangemTheme.typography.body2,
            )
        },
        textContentColor = TangemTheme.colors.text.secondary,
        confirmButton = {
            TextButton(
                onClick = config.content.confirmButtonConfig.onClick,
                content = {
                    Text(
                        text = config.content.confirmButtonConfig.text.resolveReference(),
                        style = TangemTheme.typography.button,
                        color = config.content.confirmButtonConfig.textColorProvider(),
                    )
                },
            )
        },
        dismissButton = config.content.cancelButtonConfig?.let { cancelButtonConfig ->
            {
                TextButton(
                    onClick = cancelButtonConfig.onClick,
                    content = {
                        Text(
                            text = cancelButtonConfig.text.resolveReference(),
                            style = TangemTheme.typography.button,
                            color = cancelButtonConfig.textColorProvider(),
                        )
                    },
                )
            }
        },
    )
}
