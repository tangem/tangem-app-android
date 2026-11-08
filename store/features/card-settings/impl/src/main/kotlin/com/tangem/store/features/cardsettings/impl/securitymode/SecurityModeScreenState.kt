package com.tangem.store.features.cardsettings.impl.securitymode

import com.tangem.core.ui.R
import com.tangem.store.features.cardsettings.impl.domain.SecurityOption

internal data class SecurityModeScreenState(
    val availableOptions: List<SecurityOption>,
    val selectedSecurityMode: SecurityOption,
    val isSaveChangesEnabled: Boolean,
    val onNewModeSelected: (SecurityOption) -> Unit,
    val onSaveChangesClicked: () -> Unit,
)

internal fun SecurityOption.toTitleRes(): Int {
    return when (this) {
        SecurityOption.LongTap -> R.string.details_manage_security_long_tap
        SecurityOption.PassCode -> R.string.details_manage_security_passcode
        SecurityOption.AccessCode -> R.string.details_manage_security_access_code
    }
}