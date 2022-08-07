package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.redux.PrivacySetting
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.tap.features.details.ui.common.TangemSwitch
import com.tangem.wallet.R

@Composable
fun AppSettingsScreen(
    state: AppSettingsScreenState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreensScaffold(
        content = {
            AppSettings(state = state, modifier = modifier)
        },
        titleRes = R.string.app_settings_title,
        onBackClick = onBackPressed,
    )
}

@Composable
fun AppSettings(
    state: AppSettingsScreenState,
    modifier: Modifier = Modifier,
) {
    var dialogType by remember { mutableStateOf<PrivacySetting?>(null) }
    val onDialogStateChange: (PrivacySetting?) -> Unit = { dialogType = it }

    dialogType?.let {
        SettingsAlertDialog(
            element = it,
            onDialogStateChange = onDialogStateChange,
            onSettingToggled = state.onSettingToggled,
            modifier = modifier,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AppSettingsElement(
            state = state, setting = PrivacySetting.SaveWallets,
            onDialogStateChange = onDialogStateChange,
            modifier = modifier,
        )
        Spacer(modifier = Modifier.size(32.dp))
        AppSettingsElement(
            state = state, setting = PrivacySetting.SaveAccessCode,
            onDialogStateChange = onDialogStateChange,
            modifier = modifier,
        )
    }
}

@Composable
fun AppSettingsElement(
    state: AppSettingsScreenState,
    setting: PrivacySetting,
    onDialogStateChange: (PrivacySetting?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (setting) {
        PrivacySetting.SaveWallets -> R.string.app_settings_saved_wallet
        PrivacySetting.SaveAccessCode -> R.string.app_settings_saved_access_codes
    }
    val subtitleRes = when (setting) {
        PrivacySetting.SaveWallets -> R.string.app_settings_saved_wallet_footer
        PrivacySetting.SaveAccessCode -> R.string.app_settings_saved_access_codes_footer
    }
    val checked = state.settings[setting] ?: false

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp),
        // .clickable { state.onSettingToggled(element, !checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .weight(0.6f)
                .padding(end = 4.dp),
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = TangemTypography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(id = subtitleRes),
                style = TangemTypography.body2,
                color = colorResource(id = R.color.text_secondary),
            )
        }
        TangemSwitch(
            checked = checked,
            onCheckedChange = {
                onCheckedChange(
                    element = setting,
                    enabled = it,
                    onSettingToggled = state.onSettingToggled,
                    onDialogStateChange = onDialogStateChange,
                )
            },
            modifier = modifier
                .padding(20.dp),
        )
    }
}

fun onCheckedChange(
    element: PrivacySetting, enabled: Boolean,
    onSettingToggled: (PrivacySetting, Boolean) -> Unit,
    onDialogStateChange: (PrivacySetting?) -> Unit,
) {
    //Show warning if user wants to disable the switch
    if (!enabled) {
        onDialogStateChange(element)
    } else {
        onSettingToggled(element, enabled)
    }
}

@Composable
fun SettingsAlertDialog(
    element: PrivacySetting,
    onDialogStateChange: (PrivacySetting?) -> Unit,
    onSettingToggled: (PrivacySetting, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = when (element) {
        PrivacySetting.SaveWallets -> R.string.app_settings_off_saved_wallet_alert_message
        PrivacySetting.SaveAccessCode -> R.string.app_settings_off_saved_access_code_alert_message
    }
    AlertDialog(
        onDismissRequest = { onDialogStateChange(null) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDialogStateChange(null)
                },
            ) {
                Text(
                    text = stringResource(id = R.string.common_cancel),
                    color = colorResource(id = R.color.text_secondary),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDialogStateChange(null)
                    onSettingToggled(element, false)
                },
                modifier = modifier.padding(bottom = 14.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.common_delete),
                    color = colorResource(id = R.color.text_warning),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.common_attention),
                color = colorResource(id = R.color.text_primary_1),
            )
        },
        text = {
            Text(
                text = stringResource(id = text),
                color = colorResource(id = R.color.text_secondary),
            )
        },
        shape = MaterialTheme.shapes.medium.copy(all = CornerSize(size = 28.dp)),
        modifier = modifier.padding(vertical = 34.dp, horizontal = 24.dp),
    )
}

@Preview
@Composable
fun AppSettingsScreenPreview() {
    AppSettingsScreen(
        state = AppSettingsScreenState(
            settings = mapOf(
                PrivacySetting.SaveWallets to true,
                PrivacySetting.SaveAccessCode to false,
            ),
            onSettingToggled = { _, _ -> },
        ),
        onBackPressed = { },
    )
}