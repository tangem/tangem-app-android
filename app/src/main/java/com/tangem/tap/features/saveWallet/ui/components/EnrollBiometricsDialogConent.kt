package com.tangem.tap.features.saveWallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog

@Composable
fun EnrollBiometricsDialogContent(dialog: EnrollBiometricsDialog) {
    Dialog(onDismissRequest = dialog.onCancel) {
        Column(
            modifier = Modifier
                .background(
                    shape = TangemTheme.shapes.roundedCornersLarge,
                    color = TangemTheme.colors.background.plain,
                )
                .padding(all = TangemTheme.dimens.spacing24),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.save_user_wallet_agreement_enroll_biometrics_title),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH16()
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.save_user_wallet_agreement_enroll_biometrics_description),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH24()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = dialog.onCancel,
                )
                SpacerW8()
                TextButton(
                    text = stringResource(R.string.common_enable),
                    onClick = dialog.onEnroll,
                )
            }
        }
    }
}

// region Preview
@Composable
private fun EnrollBiometricDialogContentSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        EnrollBiometricsDialogContent(dialog = EnrollBiometricsDialog({}, {}))
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EnrollBiometricDialogContentPreview_Light() {
    TangemTheme {
        EnrollBiometricDialogContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EnrollBiometricDialogContentPreview_Dark() {
    TangemTheme(isDark = true) {
        EnrollBiometricDialogContentSample()
    }
}
// endregion Preview
