package com.tangem.features.tangempay.addfunds.va.deposit

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayVirtualAccountDepositUM(
    val fees: FeesUM,
    val shouldShowTermsAndConditions: Boolean,
    val isLoading: Boolean,
    val onShowDetailsClick: () -> Unit,
    val onDismiss: () -> Unit,
    val onTermsClick: () -> Unit,
    val onPrivacyClick: () -> Unit,
) {

    @Immutable
    sealed interface FeesUM {

        data object Loading : FeesUM

        data class Content(val rows: ImmutableList<FeeRow>) : FeesUM

        /** Fees could not be loaded — an inline "tap to reload" banner is shown in place of the fee rows. */
        data class Error(val onRetryClick: () -> Unit) : FeesUM
    }

    @Immutable
    data class FeeRow(
        val title: TextReference,
        val value: String,
    )
}