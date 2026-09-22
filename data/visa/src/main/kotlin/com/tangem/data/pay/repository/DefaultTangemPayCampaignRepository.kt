package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.ImagesResponse
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class DefaultTangemPayCampaignRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayCampaignRepository {

    private val campaigns = ConcurrentHashMap<String, TangemPayCampaign>()

    override suspend fun getCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign> {
        campaigns[campaignKey]?.let { return it.right() }

        return fetchCampaign(campaignKey).onRight { campaign -> campaigns[campaignKey] = campaign }
    }

    private suspend fun fetchCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign> {
        return requestHelper.performWithStaticToken { tangemPayApi.getImages(deeplinkValue = campaignKey) }
            .map { response -> response.toCampaign(campaignKey) }
    }

    private fun ImagesResponse.toCampaign(campaignKey: String): TangemPayCampaign {
        val cardArtUrl = result.firstOrNull { it.type.equals(MAIN_IMAGE_TYPE, ignoreCase = true) }?.url
        if (cardArtUrl.isNullOrBlank()) {
            TangemLogger.i("[TangemPay][Campaign] No $MAIN_IMAGE_TYPE image for key=$campaignKey")
        }
        return TangemPayCampaign(cardArtUrl = cardArtUrl?.takeIf(String::isNotBlank))
    }

    private companion object {
        const val MAIN_IMAGE_TYPE = "MAIN"
    }
}