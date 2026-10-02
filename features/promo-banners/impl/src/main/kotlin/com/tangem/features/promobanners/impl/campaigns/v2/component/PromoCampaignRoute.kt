package com.tangem.features.promobanners.impl.campaigns.v2.component

import com.tangem.core.decompose.navigation.Route

/** Steps of the campaign sheet, stacked inside its one modal. */
internal sealed interface PromoCampaignRoute : Route {

    data object Campaign : PromoCampaignRoute

    data object ChooseToken : PromoCampaignRoute
}