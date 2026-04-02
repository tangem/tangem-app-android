package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class WalletAdditionalInfo(
    val hideable: Boolean,
    val content: Content,
    val isHotBackedUp: Boolean = false,
) {
    @Immutable
    sealed interface Content {
        data class Text(val text: TextReference) : Content
        data class SyncProgress(val progressPercent: Int) : Content
    }
}