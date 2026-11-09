package com.tangem.store.features.cardsettings.impl.securitymode.ui.state

import com.tangem.store.features.cardsettings.impl.domain.SecurityOption
import kotlinx.collections.immutable.ImmutableList

internal data class SecurityModeUM(
    val availableOptions: ImmutableList<SecurityOption>,
    val selectedSecurityMode: SecurityOption,
    val isSaveChangesEnabled: Boolean,
    val onNewModeSelected: (SecurityOption) -> Unit,
    val onSaveChangesClicked: () -> Unit,
)