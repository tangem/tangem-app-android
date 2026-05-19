package com.tangem.features.onboarding.v2.addresssync.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep

internal sealed interface AddressSyncIntent {
    data class Next(val step: AddressSyncStep) : AddressSyncIntent
    data object Sync : AddressSyncIntent
}

internal sealed class AddressSyncState {
    data object Loading : AddressSyncState()
    data class Success(
        val currencies: List<CryptoCurrency>,
        val isButtonLoading: Boolean = false,
        val shouldExit: Boolean = false,
    ) : AddressSyncState() {
        val currenciesCount: Int = currencies.size
    }
    data object Exit : AddressSyncState()
}