package com.tangem.tap.features.details.ui.securitymode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.DetailsRadioButtonElement
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
internal fun SecurityModeScreen(
    state: SecurityModeScreenState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreensScaffold(
        onBackClick = onBackClick,
        modifier = modifier,
        content = { SecurityModeOptions(state = state) },
        // titleRes = R.string.card_settings_security_mode,
    )
}

@Composable
private fun SecurityModeOptions(state: SecurityModeScreenState) {
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
            title = stringResourceSafe(id = R.string.common_save_changes),
            enabled = state.isSaveChangesEnabled,
            onClick = state.onSaveChangesClicked,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
private fun SecurityOption(option: SecurityOption, state: SecurityModeScreenState) {
    val isSelected = option == state.selectedSecurityMode

    val title = option.toTitleRes()

    val subtitle = when (option) {
        SecurityOption.LongTap -> R.string.details_manage_security_long_tap_description
        SecurityOption.PassCode -> R.string.details_manage_security_passcode_description
        SecurityOption.AccessCode -> R.string.details_manage_security_access_code_description
    }

    DetailsRadioButtonElement(
        title = stringResourceSafe(id = title),
        subtitle = stringResourceSafe(id = subtitle),
        isSelected = isSelected,
        onClick = { state.onNewModeSelected(option) },
    )
}

@Preview
@Composable
private fun SecurityModeScreenPreview() {
    SecurityModeScreen(
        state = SecurityModeScreenState(
            availableOptions = SecurityOption.entries,
            selectedSecurityMode = SecurityOption.LongTap,
            isSaveChangesEnabled = false,
            onNewModeSelected = {},
            onSaveChangesClicked = {},
        ),
        onBackClick = {},
    )
}