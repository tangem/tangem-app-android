package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CardDeliveryContext
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.domain.pay.repository.CardDeliveryQuoteRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.config.TangemPay
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockAwareCardDeliveryQuoteRepository @Inject constructor(
    private val real: DefaultCardDeliveryQuoteRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : CardDeliveryQuoteRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getCardDeliveryQuote(
        userWalletId: UserWalletId,
        context: CardDeliveryContext,
    ): Either<VisaApiError, CardDeliveryQuote> {
        if (isMockMode) return MOCK_QUOTE.right()
        return real.getCardDeliveryQuote(userWalletId, context)
    }

    private companion object {
        val MOCK_QUOTE = CardDeliveryQuote(
            country = "US",
            isPlasticAvailable = true,
            isDeliveryFeeWaived = false,
            deliveryFee = CardDeliveryQuote.DeliveryFee(
                amount = BigDecimal("5.00"),
                currency = Currency.getInstance("USD"),
            ),
            deliveryEta = CardDeliveryQuote.DeliveryEta(minBusinessDays = 5, maxBusinessDays = 10),
            hasSufficientBalance = true,
        )
    }
}