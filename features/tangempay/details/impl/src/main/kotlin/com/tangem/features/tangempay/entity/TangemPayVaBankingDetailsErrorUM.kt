package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable

/**
 * UI state for the "couldn't load banking details" bottom sheet (VA MVP0, TWI-1638).
 *
 * @property isRetryLoading whether the "Try again" button shows a loader while the payment account status
 * is being re-fetched. While `true` both actions are disabled.
 */
@Immutable
internal data class TangemPayVaBankingDetailsErrorUM(
    val isRetryLoading: Boolean,
    val onRetryClick: () -> Unit,
    val onContactSupportClick: () -> Unit,
    val onDismiss: () -> Unit,
)