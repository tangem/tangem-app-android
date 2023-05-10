package com.tangem.tap.features.welcome.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.welcome.ui.model.WarningModel
import com.tangem.wallet.R

@Composable
internal fun WarningDialog(warning: WarningModel?) {
    when (warning) {
        null -> Unit
        is WarningModel.BiometricsLockoutWarning -> {
            BasicDialog(
                title = stringResource(id = R.string.biometric_lockout_warning_title),
                message = stringResource(
                    id = if (warning.isPermanent) {
                        R.string.biometric_lockout_permanent_warning_description
                    } else {
                        R.string.biometric_lockout_warning_description
                    },
                ),
                onDismissDialog = warning.onDismiss,
                confirmButton = DialogButton(
                    title = stringResource(id = R.string.common_ok),
                    onClick = warning.onDismiss,
                ),
            )
        }
        is WarningModel.KeyInvalidatedWarning -> {
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
        is WarningModel.BiometricsDisabledWarning -> {
            BasicDialog(
                title = stringResource(id = R.string.common_warning),
                message = stringResource(id = R.string.biometric_unavailable_warning),
                onDismissDialog = warning.onDismiss,
                confirmButton = DialogButton(
                    title = stringResource(id = R.string.common_ok),
                    onClick = warning.onConfirm,
                ),
            )
        }
    }
}

// region Preview
@Composable
private fun BiometricsLockoutDialogSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WarningDialog(
            warning = WarningModel.BiometricsLockoutWarning(
                isPermanent = false,
                onDismiss = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsLockoutDialogPreview_Light() {
    TangemTheme {
        BiometricsLockoutDialogSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsLockoutDialogPreview_Dark() {
    TangemTheme(isDark = true) {
        BiometricsLockoutDialogSample()
    }
}

@Composable
private fun BiometricsLockoutDialog_Permanent_Sample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WarningDialog(
            warning = WarningModel.BiometricsLockoutWarning(
                isPermanent = true,
                onDismiss = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsLockoutDialog_Permanent_Preview_Light() {
    TangemTheme {
        BiometricsLockoutDialog_Permanent_Sample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricsLockoutDialog_Permanent_Preview_Dark() {
    TangemTheme(isDark = true) {
        BiometricsLockoutDialog_Permanent_Sample()
    }
}

@Composable
private fun KeyInvalidatedWarningSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WarningDialog(warning = WarningModel.KeyInvalidatedWarning(onDismiss = {}))
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

@Composable
private fun BiometricDisabledWarningSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WarningDialog(warning = WarningModel.BiometricsDisabledWarning(onConfirm = {}, onDismiss = {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricDisabledWarningPreview_Light() {
    TangemTheme {
        BiometricDisabledWarningSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BiometricDisabledWarningPreview_Dark() {
    TangemTheme(isDark = true) {
        BiometricDisabledWarningSample()
    }
}
// endregion Preview