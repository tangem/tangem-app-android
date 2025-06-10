package com.tangem.domain.tokens.utils

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.operations.CurrencyStatusOperations

/**
 * Proxy creator of [CryptoCurrencyStatus]. Used [CurrencyStatusOperations] to create statuses.
 *
 * @property stakingRepository staking repository
 *
[REDACTED_AUTHOR]
 */
class CurrencyStatusProxyCreator(
    private val stakingRepository: StakingRepository,
) {

    fun createCurrencyStatus(
        currency: CryptoCurrency,
        maybeQuoteStatus: Either<Error, QuoteStatus?>,
        maybeNetworkStatus: Either<Error, NetworkStatus?>,
        maybeYieldBalance: Either<Error, YieldBalance>?,
    ): Either<Error, CryptoCurrencyStatus> = either {
        var quoteRetrievingFailed = false

        val networkStatus = maybeNetworkStatus.bind()
        val quote = arrow.core.raise.recover({ maybeQuoteStatus.bind() }) {
            quoteRetrievingFailed = true
            null
        }
        val yieldBalance = maybeYieldBalance?.getOrNull()

        createCurrencyStatus(
            currency = currency,
            quoteStatus = quote,
            networkStatus = networkStatus,
            ignoreQuote = quoteRetrievingFailed,
            yieldBalance = yieldBalance,
        )
    }

    fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<Error, Set<QuoteStatus>>?,
        maybeNetworkStatuses: Either<Error, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<Error, YieldBalanceList>?,
    ): Either<Error, List<CryptoCurrencyStatus>> = either {
        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bind()?.toNonEmptySetOrNull()
        val quoteStatuses: Set<QuoteStatus>? = maybeQuotes?.fold(
            ifLeft = {
                quotesRetrievingFailed = true
                null
            },
            ifRight = {
                it.ifEmpty {
                    quotesRetrievingFailed = true
                    null
                }
            },
        )

        val yieldBalances = maybeYieldBalances?.getOrNull()

        currencies.map { currency ->
            val quote = quoteStatuses?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val address = extractAddress(networkStatus)

            val supportedIntegration = stakingRepository.getSupportedIntegrationId(currency.id)
            val yieldBalance = if (supportedIntegration.isNullOrEmpty().not()) {
                (yieldBalances as? YieldBalanceList.Data)?.getBalance(
                    address = address,
                    integrationId = supportedIntegration,
                )
            } else {
                null
            }
            createCurrencyStatus(
                currency = currency,
                quoteStatus = quote,
                networkStatus = networkStatus,
                ignoreQuote = quotesRetrievingFailed,
                yieldBalance = yieldBalance,
            )
        }
    }

    fun createCurrencyStatus(
        currency: CryptoCurrency,
        quoteStatus: QuoteStatus?,
        networkStatus: NetworkStatus?,
        ignoreQuote: Boolean,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quoteStatus = quoteStatus,
            networkStatus = networkStatus,
            ignoreQuote = ignoreQuote,
            yieldBalance = yieldBalance,
        )

        return currencyStatusOperations.createTokenStatus()
    }
}