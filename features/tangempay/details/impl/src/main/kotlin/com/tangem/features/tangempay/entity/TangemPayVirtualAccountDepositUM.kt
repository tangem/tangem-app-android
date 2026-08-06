package com.tangem.features.tangempay.entity

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

        data class Error(
            val isRetryLoading: Boolean,
            val onRetryClick: () -> Unit,
            val onContactSupportClick: () -> Unit,
        ) : FeesUM
    }

    @Immutable
    data class FeeRow(
        val title: TextReference,
        val value: String,
    )
}