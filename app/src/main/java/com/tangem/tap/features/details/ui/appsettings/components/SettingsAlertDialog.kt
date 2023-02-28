package com.tangem.tap.features.details.ui.appsettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.TextButton
import com.tangem.core.ui.components.WarningTextButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.redux.AppSetting
import com.tangem.wallet.R

@Composable
internal fun SettingsAlertDialog(
    element: AppSetting,
    onDialogStateChange: (AppSetting?) -> Unit,
    onSettingToggle: () -> Unit,
) {
    val text = when (element) {
        AppSetting.SaveWallets -> R.string.app_settings_off_saved_wallet_alert_message
        AppSetting.SaveAccessCode -> R.string.app_settings_off_saved_access_code_alert_message
    }

    AlertDialog(
        onDismissRequest = { onDialogStateChange(null) },
        confirmButton = {
            TextButton(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                text = stringResource(id = R.string.common_cancel),
                onClick = {
                    onDialogStateChange(null)
                },
            )
        },
        dismissButton = {
            WarningTextButton(
                text = stringResource(id = R.string.common_delete),
                onClick = {
                    onDialogStateChange(null)
                    onSettingToggle()
                },
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.common_attention),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.h2,
            )
        },
        text = {
            Text(
                text = stringResource(id = text),
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body2,
            )
        },
        shape = TangemTheme.shapes.roundedCornersLarge,
    )
}

// region Preview
@Composable
private fun SettingsAlertDialogSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        SettingsAlertDialog(
            element = AppSetting.SaveAccessCode,
            onDialogStateChange = {},
            onSettingToggle = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SettingsAlertDialogPreview_Light() {
    TangemTheme {
        SettingsAlertDialogSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SettingsAlertDialogPreview_Dark() {
    TangemTheme(isDark = true) {
        SettingsAlertDialogSample()
    }
}
// endregion Preview
