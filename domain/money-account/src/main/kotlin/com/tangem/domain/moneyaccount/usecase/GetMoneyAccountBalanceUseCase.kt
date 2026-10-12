package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

class GetMoneyAccountBalanceUseCase(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, MoneyAccountStatusValue.Balance> =
        Either.catch {
            val token = tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId)

            coroutineScope {
                val networkStatus = async { fetchNetworkStatus(userWalletId, token) }
                val fiatRate = async { fetchFiatRate() }

                val verified = requireNotNull(networkStatus.await()) {
                    "Money account network status is unavailable for $userWalletId"
                }

                val amount = requireNotNull(verified.amounts[token.id] as? NetworkStatus.Amount.Loaded) {
                    "Money account balance is unavailable for $userWalletId"
                }

                MoneyAccountStatusValue.Balance(
                    cryptoAmount = amount.value,
                    cryptoCurrency = token,
                    depositAddress = verified.address.defaultAddress.value,
                    currencyCode = ACCOUNT_CURRENCY_CODE,
                    fiatRate = fiatRate.await(),
                )
            }
        }

    private suspend fun fetchNetworkStatus(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
    ): NetworkStatus.Verified? {
        singleNetworkStatusFetcher(
            SingleNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                network = token.network,
                extraTokens = setOf(token),
            ),
        )

        val networkStatus = singleNetworkStatusSupplier.getSyncOrNull(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = token.network),
        )

        return networkStatus?.value as? NetworkStatus.Verified
    }

    private suspend fun fetchFiatRate(): BigDecimal? {
        singleQuoteStatusFetcher(
            SingleQuoteStatusFetcher.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID, appCurrencyId = null),
        )

        val quoteStatus = singleQuoteStatusSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID),
        )

        return (quoteStatus?.value as? QuoteStatus.Data)?.fiatRate
    }

    private companion object {
        const val ACCOUNT_CURRENCY_CODE = "USD"
    }
}