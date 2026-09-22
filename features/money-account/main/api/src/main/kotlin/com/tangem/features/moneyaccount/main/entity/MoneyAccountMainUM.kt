package com.tangem.features.moneyaccount.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed class MoneyAccountMainUM {

    data object Empty : MoneyAccountMainUM()
    data object Loading : MoneyAccountMainUM()
    data class UnderReview(val subtitle: TextReference, val onClick: () -> Unit) : MoneyAccountMainUM()
    data class Provisioning(val onClick: () -> Unit) : MoneyAccountMainUM()
    data class CountryNotSupported(val onClick: () -> Unit) : MoneyAccountMainUM()
    data class Content(
        val subtitle: TextReference,
        val isBalanceFlickering: Boolean,
        val balance: TextReference,
        val balanceSubtitle: TextReference,
        val onClick: () -> Unit,
        val shouldShowOnlyCacheWarning: Boolean,
    ) : MoneyAccountMainUM()

    data object TemporaryUnavailable : MoneyAccountMainUM()
    data object SyncNeeded : MoneyAccountMainUM()
    data object ExposedDevice : MoneyAccountMainUM()
}