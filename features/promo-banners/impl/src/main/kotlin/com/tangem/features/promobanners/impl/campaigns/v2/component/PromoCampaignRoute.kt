package com.tangem.features.promobanners.impl.campaigns.v2.component

import com.tangem.core.decompose.navigation.Route

/** Steps of the campaign sheet, stacked inside its one modal. */
internal enum class PromoCampaignRoute : Route {
    Campaign,

    ChooseToken,

    /** Replaces the whole stack, so back closes the sheet. */
    Status,
}