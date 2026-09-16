package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.config.TangemPay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockAwareTangemPayCampaignRepository @Inject constructor(
    private val real: DefaultTangemPayCampaignRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : TangemPayCampaignRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getCampaign(campaignKey: String): Either<VisaApiError, TangemPayCampaign> {
        if (isMockMode) return TangemPayCampaign(cardArtUrl = MOCK_CARD_ART_URL).right()
        return real.getCampaign(campaignKey)
    }

    private companion object {
        const val MOCK_CARD_ART_URL = "https://app.tangem.com/cards/card_default.png"
    }
}