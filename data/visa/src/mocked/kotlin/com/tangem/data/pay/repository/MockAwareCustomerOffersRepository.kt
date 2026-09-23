package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOffers
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.deeplinkOffer
import com.tangem.domain.pay.model.virtualOffer
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.config.TangemPay
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockAwareCustomerOffersRepository @Inject constructor(
    private val real: DefaultCustomerOffersRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : CustomerOffersRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getOffers(
        userWalletId: UserWalletId,
        deeplinkValue: String?,
    ): Either<VisaApiError, CustomerOffers> {
        if (!isMockMode || deeplinkValue == null) return real.getOffers(userWalletId, deeplinkValue)

        val offers = real.getOffers(userWalletId, deeplinkValue).getOrElse { CustomerOffers.EMPTY }
        if (offers.orderable.deeplinkOffer() != null) return offers.right()

        val campaignOffer = mockCampaignOffer(
            specificationName = offers.orderable.virtualOffer()?.data?.specificationName,
        )
        return offers.copy(orderable = offers.orderable + campaignOffer).right()
    }

    override suspend fun getProductInstanceOffers(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, CustomerOffers> = real.getProductInstanceOffers(userWalletId, productInstanceId)

    private fun mockCampaignOffer(specificationName: String?): Offer = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
        fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
        data = Offer.Data(
            specificationName = specificationName ?: MOCK_SPECIFICATION_NAME,
            orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        ),
        mainImageUrl = MOCK_CARD_ART_URL,
    )

    private companion object {
        const val MOCK_SPECIFICATION_NAME = "SP_000004"
        const val MOCK_CARD_ART_URL = "https://app.tangem.com/cards/card_default.png"
    }
}