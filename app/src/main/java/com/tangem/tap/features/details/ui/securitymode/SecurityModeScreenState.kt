package com.tangem.tap.features.details.ui.securitymode

import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.wallet.R

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