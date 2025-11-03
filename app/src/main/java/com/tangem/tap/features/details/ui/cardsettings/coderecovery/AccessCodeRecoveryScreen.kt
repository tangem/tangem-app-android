package com.tangem.tap.features.details.ui.cardsettings.coderecovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.DetailsRadioButtonElement
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun AccessCodeRecoveryScreen(state: AccessCodeRecoveryScreenState, onBackClick: () -> Unit) {
    SettingsScreensScaffold(
        onBackClick = onBackClick,
        content = { AccessCodeRecoveryOptions(state = state) },
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
            title = stringResourceSafe(id = R.string.common_enabled),
            subtitle = stringResourceSafe(id = R.string.card_settings_access_code_recovery_enabled_description),
            isSelected = state.isEnabledSelection,
            onClick = { state.onOptionClick(true) },
        )
        DetailsRadioButtonElement(
            title = stringResourceSafe(id = R.string.common_disabled),
            subtitle = stringResourceSafe(id = R.string.card_settings_access_code_recovery_disabled_description),
            isSelected = !state.isEnabledSelection,
            onClick = { state.onOptionClick(false) },
        )

        Spacer(modifier = Modifier.weight(1f))

        DetailsMainButton(
            title = stringResourceSafe(id = R.string.common_save_changes),
            enabled = state.isSaveChangesEnabled,
            onClick = { state.onSaveChangesClick() },
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing20),
        )
    }
}