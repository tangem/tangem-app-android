package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultTangemPayCampaignRepository @Inject constructor() : TangemPayCampaignRepository {

    private val campaigns = ConcurrentHashMap<String, TangemPayCampaign>()

    override suspend fun getCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign> {
        campaigns[campaignKey]?.let { return it.right() }

        return fetchCampaign(campaignKey).onRight { campaign -> campaigns[campaignKey] = campaign }
    }

    private fun fetchCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign> {
        TangemLogger.e("[TangemPay][Campaign] BFF has no campaign data endpoint yet, key=$campaignKey")
        return VisaApiError.Unspecified.left()
    }
}