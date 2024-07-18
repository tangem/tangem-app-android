package com.tangem.tap.features.details.ui.cardsettings.coderecovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.DetailsRadioButtonElement
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun AccessCodeRecoveryScreen(state: AccessCodeRecoveryScreenState, onBackClick: () -> Unit) {
    SettingsScreensScaffold(
        content = { AccessCodeRecoveryOptions(state = state) },
        onBackClick = onBackClick,
    )
}

@Composable
fun AccessCodeRecoveryOptions(state: AccessCodeRecoveryScreenState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = TangemTheme.dimens.spacing28),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ScreenTitle(
            titleRes = R.string.card_settings_access_code_recovery_title,
            Modifier.padding(bottom = TangemTheme.dimens.spacing36),
        )

        DetailsRadioButtonElement(
            title = stringResource(id = R.string.common_enabled),
            subtitle = stringResource(id = R.string.card_settings_access_code_recovery_enabled_description),
            selected = state.enabledSelection,
            onClick = { state.onOptionClick(true) },
        )
        DetailsRadioButtonElement(
            title = stringResource(id = R.string.common_disabled),
            subtitle = stringResource(id = R.string.card_settings_access_code_recovery_disabled_description),
            selected = !state.enabledSelection,
            onClick = { state.onOptionClick(false) },
        )

        Spacer(modifier = Modifier.weight(1f))

        DetailsMainButton(
            title = stringResource(id = R.string.common_save_changes),
            enabled = state.isSaveChangesEnabled,
            onClick = { state.onSaveChangesClick() },
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing20),
        )
    }
}
