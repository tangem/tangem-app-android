package com.tangem.features.onboarding.v2.addresssync.model

import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep

sealed interface AddressSyncIntent {
    data class Next(val step: AddressSyncStep, val replace: Boolean) : AddressSyncIntent
    data object Back : AddressSyncIntent
}