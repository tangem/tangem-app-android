package com.tangem.tap.features.saveWallet.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog

@Composable
fun EnrollBiometricsDialogContent(dialog: EnrollBiometricsDialog) {
    BasicDialog(
        title = stringResource(R.string.save_user_wallet_agreement_enroll_biometrics_title),
        message = stringResource(R.string.save_user_wallet_agreement_enroll_biometrics_description),
        confirmButton = DialogButton(
            title = stringResource(R.string.common_enable),
            onClick = dialog.onEnroll,
        ),
        dismissButton = DialogButton(
            onClick = dialog.onCancel,
        ),
        onDismissDialog = dialog.onCancel,
    )
}

// region Preview
@Composable
private fun EnrollBiometricDialogContentSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        EnrollBiometricsDialogContent(dialog = EnrollBiometricsDialog({}, {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EnrollBiometricDialogContentPreview() {
    TangemThemePreview {
        EnrollBiometricDialogContentSample()
    }
}
// endregion Preview