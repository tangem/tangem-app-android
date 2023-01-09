package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW32
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.redux.PrivacySetting
import com.tangem.tap.features.details.ui.appsettings.components.EnrollBiometricsCard
import com.tangem.tap.features.details.ui.appsettings.components.SettingsAlertDialog
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
private fun AppSettings(
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
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        if (state.showEnrollBiometricsCard) {
            EnrollBiometricsCard(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing8)
                    .fillMaxWidth(),
                onClick = state.onEnrollBiometrics,
            )
            SpacerH24()
        }

        AppSettingsElement(
            state = state,
            setting = PrivacySetting.SaveWallets,
            onDialogStateChange = onDialogStateChange,
        )
        SpacerH32()
        AppSettingsElement(
            state = state,
            setting = PrivacySetting.SaveAccessCode,
            onDialogStateChange = onDialogStateChange,
        )
    }
}

@Composable
private fun AppSettingsElement(
    modifier: Modifier = Modifier,
    state: AppSettingsScreenState,
    setting: PrivacySetting,
    onDialogStateChange: (PrivacySetting?) -> Unit,
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

    val titleTextColor by rememberUpdatedState(
        newValue = if (state.isTogglesEnabled) {
            TangemTheme.colors.text.primary1
        } else {
            TangemTheme.colors.text.secondary
        },
    )
    val descriptionTextColor by rememberUpdatedState(
        newValue = if (state.isTogglesEnabled) {
            TangemTheme.colors.text.secondary
        } else {
            TangemTheme.colors.text.tertiary
        },
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing20),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(weight = .9f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = TangemTheme.typography.subtitle1,
                color = titleTextColor,
            )
            SpacerH4()
            Text(
                text = stringResource(id = subtitleRes),
                style = TangemTheme.typography.body2,
                color = descriptionTextColor,
            )
        }
        SpacerW32()
        TangemSwitch(
            checked = checked,
            enabled = state.isTogglesEnabled,
            onCheckedChange = { isChecked ->
                onCheckedChange(
                    element = setting,
                    enabled = isChecked,
                    onSettingToggled = state.onSettingToggled,
                    onDialogStateChange = onDialogStateChange,
                )
            },
        )
    }
}

private fun onCheckedChange(
    element: PrivacySetting,
    enabled: Boolean,
    onSettingToggled: (PrivacySetting, Boolean) -> Unit,
    onDialogStateChange: (PrivacySetting?) -> Unit,
) {
    //Show warning if user wants to disable the switch
    if (!enabled) {
        onDialogStateChange(element)
    } else {
        onSettingToggled(element, true)
    }
}

// region Preview
@Composable
private fun AppSettingsScreenSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        AppSettingsScreen(
            state = AppSettingsScreenState(
                settings = mapOf(
                    PrivacySetting.SaveWallets to true,
                    PrivacySetting.SaveAccessCode to false,
                ),
                showEnrollBiometricsCard = false,
                isTogglesEnabled = true,
                onSettingToggled = { _, _ -> },
                onEnrollBiometrics = {},
            ),
            onBackPressed = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreenPreview_Light() {
    TangemTheme {
        AppSettingsScreenSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreenPreview_Dark() {
    TangemTheme(isDark = true) {
        AppSettingsScreenSample()
    }
}

@Composable
private fun AppSettingsScreen_EnrollBiometrics_Sample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary),
    ) {
        AppSettingsScreen(
            state = AppSettingsScreenState(
                settings = mapOf(
                    PrivacySetting.SaveWallets to true,
                    PrivacySetting.SaveAccessCode to false,
                ),
                showEnrollBiometricsCard = true,
                isTogglesEnabled = false,
                onSettingToggled = { _, _ -> },
                onEnrollBiometrics = {},
            ),
            onBackPressed = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreen_EnrollBiometrics_Preview_Light() {
    TangemTheme {
        AppSettingsScreen_EnrollBiometrics_Sample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AppSettingsScreen_EnrollBiometrics_Preview_Dark() {
    TangemTheme(isDark = true) {
        AppSettingsScreen_EnrollBiometrics_Sample()
    }
}
// endregion Preview
