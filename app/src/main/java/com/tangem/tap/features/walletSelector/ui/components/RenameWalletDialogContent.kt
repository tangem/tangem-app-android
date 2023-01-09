package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.walletSelector.ui.model.RenameWalletDialog
import com.tangem.wallet.R

@Composable
internal fun RenameWalletDialogContent(
    modifier: Modifier = Modifier,
    dialog: RenameWalletDialog,
) {
    Dialog(onDismissRequest = dialog.onCancel) {
        Column(
            modifier = modifier
                .background(
                    shape = TangemTheme.shapes.roundedCornersLarge,
                    color = TangemTheme.colors.background.plain,
                )
                .padding(all = TangemTheme.dimens.spacing24),
        ) {
            var value by remember {
                mutableStateOf(TextFieldValue(text = dialog.currentName))
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.user_wallet_list_rename_popup_title),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH16()
            OutlineTextField(
                value = value,
                label = stringResource(R.string.user_wallet_list_rename_popup_placeholder),
                onValueChange = { newValue ->
                    value = newValue
                },
            )
            SpacerH24()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(id = R.string.common_cancel),
                    onClick = dialog.onCancel,
                )
                SpacerW8()
                TextButton(
                    text = stringResource(id = R.string.common_save),
                    enabled = value.text.isNotEmpty() && value.text != dialog.currentName,
                    onClick = {
                        dialog.onApply(value.text)
                    },
                )
            }
        }
    }
}

// region Preview
@Composable
private fun RenameWalletDialogContentSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        RenameWalletDialogContent(dialog = RenameWalletDialog("", {}, {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Light() {
    TangemTheme {
        RenameWalletDialogContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RenameWalletDialogContentPreview_Dark() {
    TangemTheme(isDark = true) {
        RenameWalletDialogContentSample()
    }
}
// endregion Preview
