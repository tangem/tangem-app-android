package com.tangem.features.onboarding.v2.addresssync.navigation

import androidx.annotation.StringRes
import com.tangem.features.onboarding.v2.impl.R

enum class AddressSyncStep(val pageNumber: Int, @StringRes val stringId: Int) {
    ASK_BIOMETRY(pageNumber = 1, stringId = R.string.onboarding_navbar_title_biometrics),
    ASK_NOTIFICATIONS(pageNumber = 2, stringId = R.string.onboarding_title_notifications),
    ADDRESS_SYNC(pageNumber = 3, stringId = R.string.onboarding_navbar_title_last_step),
}