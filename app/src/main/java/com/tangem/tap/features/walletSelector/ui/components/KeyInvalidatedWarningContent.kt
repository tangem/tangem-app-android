package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.walletSelector.ui.model.WarningModel
import com.tangem.wallet.R

@Composable
internal fun KeyInvalidatedWarningContent(warning: WarningModel.KeyInvalidatedWarning) {
    BasicDialog(
        title = stringResource(id = R.string.common_attention),
        message = stringResource(id = R.string.key_invalidated_warning_description),
        onDismissDialog = warning.onDismiss,
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = warning.onDismiss,
        ),
    )
}

// region Preview
@Composable
private fun KeyInvalidatedWarningSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        KeyInvalidatedWarningContent(
            warning = WarningModel.KeyInvalidatedWarning(onDismiss = {}),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun KeyInvalidatedWarningPreview_Light() {
    TangemTheme {
        KeyInvalidatedWarningSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun KeyInvalidatedWarningPreview_Dark() {
    TangemTheme(isDark = true) {
        KeyInvalidatedWarningSample()
    }
}
// endregion Preview
