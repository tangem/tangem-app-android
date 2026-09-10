package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayCampaignRepository {

    suspend fun getCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign>
}