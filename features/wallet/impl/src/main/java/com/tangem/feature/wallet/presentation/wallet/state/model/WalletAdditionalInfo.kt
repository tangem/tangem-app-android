package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class WalletAdditionalInfo(
    val hideable: Boolean,
    val content: TextReference,
)