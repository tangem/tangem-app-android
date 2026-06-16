package com.tangem.features.commonfeatures.impl.userportfolio.model

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM

@Immutable
internal data class UserPortfolioUM(
    val content: TokenSelectorContentUM,
    val isAddEnabled: Boolean,
)