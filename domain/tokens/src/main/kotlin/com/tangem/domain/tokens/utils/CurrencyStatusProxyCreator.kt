package com.tangem.domain.tokens.utils

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.toNonEmptySetOrNull
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.staking.model.stakekit.YieldBalanceList
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
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
        maybeQuote: Either<Error, Quote?>,
        maybeNetworkStatus: Either<Error, NetworkStatus?>,
        maybeYieldBalance: Either<Error, YieldBalance>?,
    ): Either<Error, CryptoCurrencyStatus> = either {
        var quoteRetrievingFailed = false

        val networkStatus = maybeNetworkStatus.bind()
        val quote = arrow.core.raise.recover({ maybeQuote.bind() }) {
            quoteRetrievingFailed = true
            null
        }
        val yieldBalance = maybeYieldBalance?.getOrNull()

        createCurrencyStatus(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = quoteRetrievingFailed,
            yieldBalance = yieldBalance,
        )
    }

    fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<Error, Set<Quote>>?,
        maybeNetworkStatuses: Either<Error, Set<NetworkStatus>>?,
        maybeYieldBalances: Either<Error, YieldBalanceList>?,
    ): Either<Error, List<CryptoCurrencyStatus>> = either {
        var quotesRetrievingFailed = false

        val networksStatuses = maybeNetworkStatuses?.bind()?.toNonEmptySetOrNull()
        val quotes: Set<Quote>? = maybeQuotes?.fold(
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
            val quote = quotes?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
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
                quote = quote,
                networkStatus = networkStatus,
                ignoreQuote = quotesRetrievingFailed,
                yieldBalance = yieldBalance,
            )
        }
    }

    fun createCurrencyStatus(
        currency: CryptoCurrency,
        quote: Quote?,
        networkStatus: NetworkStatus?,
        ignoreQuote: Boolean,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus {
        val currencyStatusOperations = CurrencyStatusOperations(
            currency = currency,
            quote = quote,
            networkStatus = networkStatus,
            ignoreQuote = ignoreQuote,
            yieldBalance = yieldBalance,
        )

        return currencyStatusOperations.createTokenStatus()
    }
}