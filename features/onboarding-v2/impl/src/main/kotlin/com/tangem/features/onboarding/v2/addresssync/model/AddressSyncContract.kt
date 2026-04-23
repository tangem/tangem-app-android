package com.tangem.features.onboarding.v2.addresssync.model

import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep

internal sealed interface AddressSyncIntent {
    data class Next(val step: AddressSyncStep) : AddressSyncIntent
    data object Sync : AddressSyncIntent
}

internal sealed class AddressSyncState {
    data object Loading : AddressSyncState()
    data class Success(val currenciesCount: Int) : AddressSyncState()
    data object NoTokens : AddressSyncState()
}