package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.tangempay.model.TangemPayCampaign
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayCampaignRepository {

    suspend fun getCampaign(campaignKey: String): Either<TangemPayApiError, TangemPayCampaign>
}