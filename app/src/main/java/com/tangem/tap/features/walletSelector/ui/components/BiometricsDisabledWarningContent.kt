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
internal fun BiometricsDisabledWarningContent(warning: WarningModel.BiometricsDisabledWarning) {
    BasicDialog(
        title = stringResource(id = R.string.common_warning),
        message = stringResource(id = R.string.biometric_unavailable_warning),
        onDismissDialog = warning.onDismiss,
        isDismissable = false,
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = warning.onDismiss,
        ),
    )
}

// region Preview
@Composable
private fun BiometricsDisabledWarningContentSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BiometricsDisabledWarningContent(
            warning = WarningModel.BiometricsDisabledWarning(
                onDismiss = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsDisabledWarningContentPreview_Light() {
    TangemTheme {
        BiometricsDisabledWarningContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsDisabledWarningContentPreview_Dark() {
    TangemTheme(isDark = true) {
        BiometricsDisabledWarningContentSample()
    }
}
// endregion Preview