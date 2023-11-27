package com.tangem.feature.swap.models

import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.domain.SwapProvider

data class SwapSuccessStateHolder(
    val timestamp: Long,
    val txUrl: String,
    val fee: TextReference,
    val rate: TextReference,
    val selectedProvider: SwapProvider,
    val fromTokenAmount: TextReference,
    val toTokenAmount: TextReference,
    val fromTokenFiatAmount: TextReference,
    val toTokenFiatAmount: TextReference,
    val fromTokenIconState: TokenIconState?,
    val toTokenIconState: TokenIconState?,
    val onSecondaryButtonClick: () -> Unit,
)