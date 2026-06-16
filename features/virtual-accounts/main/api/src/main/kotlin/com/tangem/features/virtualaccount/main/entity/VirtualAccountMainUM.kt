package com.tangem.features.virtualaccount.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for the Virtual Account main-screen block.
 *
 * Mirrors `TangemPayMainUM` but carries virtual-account-specific states (no card-related variants).
 */
@Immutable
sealed class VirtualAccountMainUM {

    data object Empty : VirtualAccountMainUM()
    data object Loading : VirtualAccountMainUM()
    data class UnderReview(val subtitle: TextReference, val onClick: () -> Unit) : VirtualAccountMainUM()
    data class Provisioning(val onClick: () -> Unit) : VirtualAccountMainUM()
    data class CountryNotSupported(val onClick: () -> Unit) : VirtualAccountMainUM()
    data class Content(
        val subtitle: TextReference,
        val isBalanceFlickering: Boolean,
        val balance: TextReference,
        val balanceSubtitle: TextReference,
        val onClick: () -> Unit,
        val shouldShowOnlyCacheWarning: Boolean,
    ) : VirtualAccountMainUM()

    data object TemporaryUnavailable : VirtualAccountMainUM()
    data object SyncNeeded : VirtualAccountMainUM()
    data object ExposedDevice : VirtualAccountMainUM()
}