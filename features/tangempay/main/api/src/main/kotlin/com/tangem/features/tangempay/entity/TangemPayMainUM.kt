package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed class TangemPayMainUM {

    data object Empty : TangemPayMainUM()
    data object Loading : TangemPayMainUM()
    data class UnderReview(val subtitle: TextReference, val onClick: () -> Unit) : TangemPayMainUM()
    data class IssuingCard(val onClick: () -> Unit) : TangemPayMainUM()
    data class FailedToIssue(val onClick: () -> Unit) : TangemPayMainUM()
    data class Content(
        val subtitle: TextReference,
        val isBalanceFlickering: Boolean,
        val balance: TextReference,
        val balanceSubtitle: TextReference,
        val onClick: () -> Unit,
        val shouldShowOnlyCacheWarning: Boolean,
    ) : TangemPayMainUM()

    data object TemporaryUnavailable : TangemPayMainUM()
    data object SyncNeeded : TangemPayMainUM()
    data object ExposedDevice : TangemPayMainUM()
}