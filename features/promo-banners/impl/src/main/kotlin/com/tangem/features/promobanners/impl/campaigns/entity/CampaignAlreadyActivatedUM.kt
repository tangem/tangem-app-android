package com.tangem.features.promobanners.impl.campaigns.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState

@Immutable
internal data class CampaignAlreadyActivatedUM(
    val selectedToken: TokenItemState,
    val selectedAccount: SelectedAccountUM?,
)