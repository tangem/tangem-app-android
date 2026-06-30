package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * UI state of the bank-transfer deposit bottom sheet (VA MVP0, TWI-1638).
 *
 * @property shouldShowTermsAndConditions `true` for the `Eligible` state — shows the provider T&C consent footer.
 */
@Immutable
internal data class TangemPayVirtualAccountDepositUM(
    val fees: ImmutableList<FeeRow>,
    val shouldShowTermsAndConditions: Boolean,
    val onShowDetailsClick: () -> Unit,
    val onDismiss: () -> Unit,
    val onTermsClick: () -> Unit,
    val onPrivacyClick: () -> Unit,
) {

    @Immutable
    data class FeeRow(
        val title: TextReference,
        val value: String,
    )
}