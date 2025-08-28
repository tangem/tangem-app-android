package com.tangem.domain.tokens.wallet

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.wallet.implementor.MultiWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletWithTokenBalanceFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * Fetcher of wallet balance by [UserWalletId]
 *
 * @property currenciesRepository                currencies repository
 * @property multiWalletBalanceFetcher           balance fetcher of multi-currency wallet
 * @property singleWalletWithTokenBalanceFetcher balance fetcher of single-currency wallet with token
 * @property singleWalletBalanceFetcher          balance fetcher of single-currency wallet
 * @property multiNetworkStatusFetcher           networks statuses fetcher
 * @property multiQuoteStatusFetcher             quotes statuses fetcher
 * @property multiYieldBalanceFetcher            yields balances fetcher
 * @property dispatchers                         dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class WalletBalanceFetcher internal constructor(
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletBalanceFetcher: BaseWalletBalanceFetcher,
    private val singleWalletWithTokenBalanceFetcher: BaseWalletBalanceFetcher,
    private val singleWalletBalanceFetcher: BaseWalletBalanceFetcher,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : FlowFetcher<WalletBalanceFetcher.Params> {

    /** Additional constructor without internal dependencies */
    constructor(
        currenciesRepository: CurrenciesRepository,
        multiWalletCryptoCurrenciesFetcher: MultiWalletCryptoCurrenciesFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ) : this(
        currenciesRepository = currenciesRepository,
        multiWalletBalanceFetcher = MultiWalletBalanceFetcher(
            multiWalletCryptoCurrenciesFetcher = multiWalletCryptoCurrenciesFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        ),
        singleWalletWithTokenBalanceFetcher = SingleWalletWithTokenBalanceFetcher(
            currenciesRepository = currenciesRepository,
        ),
        singleWalletBalanceFetcher = SingleWalletBalanceFetcher(currenciesRepository = currenciesRepository),
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        multiYieldBalanceFetcher = multiYieldBalanceFetcher,
        stakingIdFactory = stakingIdFactory,
        dispatchers = dispatchers,
    )

    override suspend fun invoke(params: Params) = Either.catchOn(dispatchers.default) {
        val userWalletId = params.userWalletId
        val cardTypesResolver = currenciesRepository.getCardTypesResolver(userWalletId = userWalletId)

        val fetcher = when {
            cardTypesResolver == null || cardTypesResolver.isMultiwalletAllowed() -> multiWalletBalanceFetcher
            cardTypesResolver.isSingleWalletWithToken() -> singleWalletWithTokenBalanceFetcher
            cardTypesResolver.isSingleWallet() -> singleWalletBalanceFetcher
            else -> error("Unknown type of wallet: $userWalletId")
        }

        val currencies = fetcher.getCryptoCurrencies(userWalletId = userWalletId).ifEmpty {
            error("UserWallet doesn't contain crypto-currencies: $userWalletId")
        }

        fetcher.fetch(userWalletId = userWalletId, currencies = currencies)
    }

    private suspend fun BaseWalletBalanceFetcher.fetch(userWalletId: UserWalletId, currencies: Set<CryptoCurrency>) {
        coroutineScope {
            val results = fetchingSources.map { source ->
                async {
                    val maybeResult = when (source) {
                        FetchingSource.NETWORK -> fetchNetworks(userWalletId = userWalletId, currencies = currencies)
                        FetchingSource.QUOTE -> fetchQuotes(currencies = currencies)
                        FetchingSource.STAKING -> fetchStaking(userWalletId = userWalletId, currencies = currencies)
                    }

                    source to maybeResult
                }
            }
                .awaitAll()

            val errors = results.mapNotNull { (source, maybeResult) ->
                val error = maybeResult.leftOrNull() ?: return@mapNotNull null

                source to error
            }

            check(errors.isEmpty()) {
                val message = "Failed to fetch next sources for $userWalletId:\n" +
                    errors.joinToString(separator = "\n") { "${it.first.name} – ${it.second}" }

                Timber.e(message)

                message
            }
        }
    }

    private suspend fun fetchNetworks(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
            ),
        )
    }

    private suspend fun fetchQuotes(currencies: Set<CryptoCurrency>): Either<Throwable, Unit> {
        return multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
                appCurrencyId = null,
            ),
        )
    }

    private suspend fun fetchStaking(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
    ): Either<Throwable, Unit> = either {
        val maybeStakingIds = currencies.map {
            val stakingId = stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it)

            if (stakingId.isLeft { it is StakingIdFactory.Error.UnableToGetAddress }) {
                Timber.e("Unable to get staking ID for user wallet $userWalletId and currency ${it.id}")
            }

            stakingId
        }

        val stakingIds = maybeStakingIds.mapNotNullTo(hashSetOf()) { it.getOrNull() }

        if (stakingIds.isNotEmpty()) {
            multiYieldBalanceFetcher(
                params = MultiYieldBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
            )
                .bind()
        } else {
            Timber.i("No staking IDs found for user wallet $userWalletId with currencies: $currencies")
        }
    }

    /**
     * Params of [WalletBalanceFetcher]
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId)
}