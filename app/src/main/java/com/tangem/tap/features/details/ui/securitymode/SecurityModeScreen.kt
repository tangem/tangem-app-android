package com.tangem.tap.features.details.ui.securitymode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun SecurityModeScreen(state: SecurityModeScreenState, onBackClick: () -> Unit) {
    SettingsScreensScaffold(
        content = { SecurityModeOptions(state = state) },
        // titleRes = R.string.card_settings_security_mode,
        onBackClick = onBackClick,
    )
}

@Composable
fun SecurityModeOptions(state: SecurityModeScreenState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ScreenTitle(titleRes = R.string.card_settings_security_mode, Modifier.padding(bottom = 36.dp))

        state.availableOptions.map {
            SecurityOption(option = it, state = state)
        }

        Spacer(modifier = Modifier.weight(1f))

        DetailsMainButton(
            title = stringResource(id = R.string.common_save_changes),
            enabled = state.isSaveChangesEnabled,
            onClick = state.onSaveChangesClicked,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
fun SecurityOption(option: SecurityOption, state: SecurityModeScreenState) {
    val selected = option == state.selectedSecurityMode

    val title = option.toTitleRes()

    val subtitle = when (option) {
        SecurityOption.LongTap -> R.string.details_manage_security_long_tap_description
        SecurityOption.PassCode -> R.string.details_manage_security_passcode_description
        SecurityOption.AccessCode -> R.string.details_manage_security_access_code_description
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { state.onNewModeSelected(option) },
            )
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(end = 20.dp),
            colors = RadioButtonDefaults.colors(
                unselectedColor = colorResource(id = R.color.icon_secondary),
                selectedColor = colorResource(id = R.color.icon_accent),
            ),
        )

        Column {
            Text(
                text = stringResource(id = title),
                style = TangemTypography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(id = subtitle),
                style = TangemTypography.body2,
                color = colorResource(id = R.color.text_secondary),
            )
        }
    }
}

@Preview
@Composable
private fun SecurityModeScreenPreview() {
    SecurityModeScreen(
        state = SecurityModeScreenState(
            availableOptions = SecurityOption.values().toList(),
            selectedSecurityMode = SecurityOption.LongTap,
            isSaveChangesEnabled = false,
            onNewModeSelected = {},
            onSaveChangesClicked = {},
        ),
        onBackClick = {},
    )
}
