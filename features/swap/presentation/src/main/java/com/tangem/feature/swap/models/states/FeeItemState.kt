package com.tangem.feature.swap.models.states

import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.ui.FeeType

sealed class FeeItemState {

    data class Content(
        val feeType: FeeType,
        val title: TextReference,
        val amountCrypto: String,
        val symbolCrypto: String,
        val amountFiatFormatted: String,
        val isClickable: Boolean,
        val onClick: () -> Unit,
    ) : FeeItemState()

    object Empty : FeeItemState()
}
