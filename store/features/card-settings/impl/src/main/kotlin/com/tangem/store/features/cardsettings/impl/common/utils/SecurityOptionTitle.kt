package com.tangem.store.features.cardsettings.impl.common.utils

import androidx.annotation.StringRes
import com.tangem.core.ui.R
import com.tangem.store.features.cardsettings.impl.domain.SecurityOption

@StringRes
internal fun SecurityOption.toTitleRes(): Int = when (this) {
    SecurityOption.LongTap -> R.string.details_manage_security_long_tap
    SecurityOption.PassCode -> R.string.details_manage_security_passcode
    SecurityOption.AccessCode -> R.string.details_manage_security_access_code
}