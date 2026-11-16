package com.tangem.domain.pay.usecase

import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import java.math.BigDecimal

class GetTangemPayFiatRateUseCase(
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
) {
    suspend operator fun invoke(): BigDecimal? {
        val quoteStatus = singleQuoteStatusSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
        )?.value as? QuoteStatus.Data
        return quoteStatus?.fiatRate
    }
}