package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the campaign activation bottom sheet.
 *
 * The intro promo screen (image + title + description) is always shown. When [selectedToken] is `null`
 * the footer shows "Select token"; once a token is chosen it shows the account block, the terms agreement
 * and the "Enroll" button. When [isChoosingToken] is `true` the token selector is shown on top of the
 * intro (as a stacked bottom sheet), not instead of it.
 */
internal data class ActivateCampaignUM(
    val campaignName: String,
    val title: TextReference,
    val description: TextReference,
    val selectedToken: TokenItemState?,
    val isChoosingToken: Boolean,
)