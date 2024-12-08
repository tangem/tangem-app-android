package com.tangem.tap.features.welcome.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.welcome.ui.model.WarningModel
import com.tangem.wallet.R

@Composable
internal fun WarningDialog(warning: WarningModel?) {
    when (warning) {
        null -> Unit
        is WarningModel.BiometricsLockoutWarning -> {
            BasicDialog(
                title = stringResourceSafe(id = R.string.biometric_lockout_warning_title),
                message = stringResourceSafe(
                    id = if (warning.isPermanent) {
                        R.string.biometric_lockout_permanent_warning_description
                    } else {
                        R.string.biometric_lockout_warning_description
                    },
                ),
                onDismissDialog = warning.onDismiss,
                confirmButton = DialogButtonUM(
                    title = stringResourceSafe(id = R.string.common_ok),
                    onClick = warning.onDismiss,
                ),
            )
        }
        is WarningModel.KeyInvalidatedWarning -> {
            BasicDialog(
                title = stringResourceSafe(id = R.string.common_attention),
                message = stringResourceSafe(id = R.string.key_invalidated_warning_description),
                onDismissDialog = warning.onDismiss,
                confirmButton = DialogButtonUM(
                    title = stringResourceSafe(id = R.string.common_ok),
                    onClick = warning.onDismiss,
                ),
            )
        }
        is WarningModel.BiometricsDisabledWarning -> {
            BasicDialog(
                title = stringResourceSafe(id = R.string.common_warning),
                message = stringResourceSafe(id = R.string.biometric_unavailable_warning),
                onDismissDialog = warning.onDismiss,
                isDismissable = false,
                confirmButton = DialogButtonUM(
                    title = stringResourceSafe(id = R.string.common_ok),
                    onClick = warning.onDismiss,
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
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BiometricsLockoutDialogPreview() {
    TangemThemePreview {
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
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BiometricsLockoutDialog_Permanent_Preview() {
    TangemThemePreview {
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
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KeyInvalidatedWarningPreview() {
    TangemThemePreview {
        KeyInvalidatedWarningSample()
    }
}

@Composable
private fun BiometricDisabledWarningSample(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WarningDialog(warning = WarningModel.BiometricsDisabledWarning(onDismiss = {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BiometricDisabledWarningPreview() {
    TangemThemePreview {
        BiometricDisabledWarningSample()
    }
}
// endregion Preview