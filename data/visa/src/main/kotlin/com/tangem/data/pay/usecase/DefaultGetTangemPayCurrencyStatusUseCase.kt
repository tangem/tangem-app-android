package com.tangem.data.pay.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultGetTangemPayCurrencyStatusUseCase @Inject constructor(
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
) : GetTangemPayCurrencyStatusUseCase {

    override suspend fun invoke(
        currency: CryptoCurrency,
        cryptoAmount: BigDecimal,
        fiatAmount: BigDecimal,
        depositAddress: String,
    ): CryptoCurrencyStatus? {
        val rawCurrencyId = currency.id.rawCurrencyId ?: return null
        val quoteStatus = singleQuoteStatusSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
        )?.value
        val quoteStatusData: QuoteStatus.Data = when (quoteStatus) {
            is QuoteStatus.Data -> quoteStatus
            else -> {
                singleQuoteStatusFetcher.invoke(
                    params = SingleQuoteStatusFetcher.Params(rawCurrencyId = rawCurrencyId, appCurrencyId = null),
                )
                singleQuoteStatusSupplier.getSyncOrNull(
                    params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
                )?.value as? QuoteStatus.Data
            }
        } ?: return null

        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = cryptoAmount,
                fiatRate = quoteStatusData.fiatRate,
                priceChange = quoteStatusData.priceChange,
                fiatAmount = fiatAmount,
                networkAddress = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        type = NetworkAddress.Address.Type.Primary,
                        value = depositAddress,
                    ),
                ),
                sources = CryptoCurrencyStatus.Sources(),
                pendingTransactions = emptySet(),
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
            ),
        )
    }
}