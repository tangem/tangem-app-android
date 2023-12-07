package com.tangem.feature.swap.models

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference

data class SwapSuccessStateHolder(
    val timestamp: Long,
    val txUrl: String,
    val fee: TextReference,
    val rate: TextReference,
    val showStatusButton: Boolean,
    val providerName: TextReference,
    val providerType: TextReference,
    val providerIcon: String,
    val fromTokenAmount: TextReference,
    val toTokenAmount: TextReference,
    val fromTokenFiatAmount: TextReference,
    val toTokenFiatAmount: TextReference,
    val fromTokenIconState: TokenIconState?,
    val toTokenIconState: TokenIconState?,
    val onExploreButtonClick: () -> Unit,
    val onStatusButtonClick: () -> Unit,
)
