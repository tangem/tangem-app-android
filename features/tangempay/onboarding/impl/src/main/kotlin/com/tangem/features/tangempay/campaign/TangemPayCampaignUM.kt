package com.tangem.features.tangempay.campaign

import javax.annotation.concurrent.Immutable

@Immutable
internal sealed interface TangemPayCampaignUM {

    data object Loading : TangemPayCampaignUM
}