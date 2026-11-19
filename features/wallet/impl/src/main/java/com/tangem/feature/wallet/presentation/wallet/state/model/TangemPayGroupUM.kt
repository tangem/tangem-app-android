package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface TangemPayGroupUM {

    data object Hidden : TangemPayGroupUM

    data class Content(
        val id: String,
        val title: TextReference,
        val label: TextReference,
        val balance: TextReference,
        val balanceCaption: TextReference,
        val isBalanceFlickering: Boolean,
        val isExpanded: Boolean,
        val onClick: () -> Unit,
    ) : TangemPayGroupUM
}