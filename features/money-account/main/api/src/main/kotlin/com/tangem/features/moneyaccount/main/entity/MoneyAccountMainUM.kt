package com.tangem.features.moneyaccount.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface MoneyAccountMainUM {

    data object Empty : MoneyAccountMainUM

    data object Loading : MoneyAccountMainUM

    data class Onboarding(
        val subtitle: TextReference,
        val onClick: () -> Unit,
    ) : MoneyAccountMainUM

    data class ActionRequired(
        val balance: BalanceUM?,
        val onClick: () -> Unit,
    ) : MoneyAccountMainUM

    data class Content(
        val subtitle: TextReference,
        val balance: BalanceUM,
        val onClick: () -> Unit,
    ) : MoneyAccountMainUM

    data object Unavailable : MoneyAccountMainUM

    @Immutable
    data class BalanceUM(
        val amount: TextReference,
        val currency: TextReference,
        val isFlickering: Boolean,
        val isFromCache: Boolean,
    )
}