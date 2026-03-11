package com.tangem.domain.tokens.operations

import arrow.core.*
import arrow.core.raise.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.utils.CurrencyStatusProxyCreator
import kotlinx.coroutines.flow.firstOrNull

/**
 * Base operations for working with currency status
 *
 * @property currenciesRepository repository for currencies
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList")
class BaseCurrencyStatusOperations(
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleStakingBalanceSupplier: SingleStakingBalanceSupplier,
    private val multiStakingBalanceSupplier: MultiStakingBalanceSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val stakingIdFactory: StakingIdFactory,
) {

    private val currencyStatusProxyCreator = CurrencyStatusProxyCreator()

    suspend fun getNetworkCoinSync(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<Error, CryptoCurrencyStatus> {
        val currency = recover(
            block = { getNetworkCoin(userWalletId, networkId, derivationPath) },
            recover = { return it.left() },
        )

        return getCurrencyStatusSync(userWalletId, currency.id)
    }

    suspend fun getCurrencyStatusSync(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
        isSingleWalletWithTokens: Boolean = false,
    ): Either<Error, CryptoCurrencyStatus> {
        return either {
            catch(
                block = {
                    val currency = if (isSingleWalletWithTokens) {
                        currenciesRepository.getSingleCurrencyWalletWithCardCurrency(userWalletId, cryptoCurrencyId)
                    } else {
                        getMultiCurrencyWalletCurrency(userWalletId, cryptoCurrencyId)
                    }

                    val quote = cryptoCurrencyId.rawCurrencyId?.let { rawId ->
                        singleQuoteStatusSupplier(params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawId))
                            .firstOrNull()
                    }
                        ?.right()
                        ?: Error.EmptyQuotes.left()

                    val networkStatuses = singleNetworkStatusSupplier(
                        params = SingleNetworkStatusProducer.Params(
                            userWalletId = userWalletId,
                            network = currency.network,
                        ),
                    )
                        .firstOrNull()
                        .right()

                    val stakingBalances = getStakingBalanceSync(userWalletId, currency)

                    return currencyStatusProxyCreator.createCurrencyStatus(
                        currency = currency,
                        maybeQuoteStatus = quote,
                        maybeNetworkStatus = networkStatuses,
                        maybeStakingBalance = stakingBalances,
                    )
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    suspend fun getCurrenciesStatusesSync(userWalletId: UserWalletId): Either<Error, List<CryptoCurrencyStatus>> {
        return either {
            catch(
                block = {
                    val nonEmptyCurrencies = multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                        params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                    )
                        ?.toNonEmptyListOrNull()
                        ?: return emptyList<CryptoCurrencyStatus>().right()

                    val (_, currenciesIds) = getIds(nonEmptyCurrencies)
                    val rawIds = currenciesIds.mapNotNull { it.rawCurrencyId }.toSet()

                    val quotes = quotesRepository.getMultiQuoteSyncOrNull(currenciesIds = rawIds)?.right()

                    val networkStatuses = multiNetworkStatusSupplier(
                        params = MultiNetworkStatusProducer.Params(userWalletId = userWalletId),
                    )
                        .firstOrNull()
                        .orEmpty()
                        .right()

                    val stakingBalances = getStakingBalancesSync(userWalletId, nonEmptyCurrencies)

                    return currencyStatusProxyCreator.createCurrenciesStatuses(
                        currencies = nonEmptyCurrencies,
                        maybeQuotes = quotes,
                        maybeNetworkStatuses = networkStatuses,
                        maybeStakingBalances = stakingBalances,
                    )
                },
                catch = { raise(Error.DataError(it)) },
            )
        }
    }

    private suspend fun Raise<Error>.getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
    ): CryptoCurrency {
        return Either.catch {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                ?.firstOrNull { it.id == currencyId }
                ?: error("Unable to find currency with ID: $currencyId")
        }
            .mapLeft(Error::DataError)
            .bind()
    }

    private suspend fun Raise<Error>.getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency {
        return Either.catch {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                ?.filterIsInstance<CryptoCurrency.Coin>()
                ?.firstOrNull { it.network.id == networkId && it.network.derivationPath == derivationPath }
                ?: error("Unable to create network coin with ID: $networkId")
        }
            .mapLeft { Error.DataError(it) }
            .bind()
    }

    private suspend fun getStakingBalancesSync(
        userWalletId: UserWalletId,
        cryptoCurrencies: List<CryptoCurrency>,
    ): Either<Error.EmptyStakingBalances, List<StakingBalance>> = either {
        val stakingIds = cryptoCurrencies.mapNotNull { cryptoCurrency ->
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrency)
                .getOrNull()
        }

        ensure(stakingIds.isNotEmpty()) { Error.EmptyStakingBalances }

        val balances = multiStakingBalanceSupplier.getSyncOrNull(
            params = MultiStakingBalanceProducer.Params(userWalletId = userWalletId),
        )
            .orEmpty()
            .filter { it.stakingId in stakingIds }

        ensure(balances.isNotEmpty()) { Error.EmptyStakingBalances }

        balances
    }

    private suspend fun getStakingBalanceSync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<Error, StakingBalance> = either {
        val stakingId = stakingIdFactory.create(userWalletId, cryptoCurrency)
            .mapLeft {
                val exception = IllegalStateException("$it")
                Error.DataError(exception)
            }
            .bind()

        val yieldBalance = singleStakingBalanceSupplier.getSyncOrNull(
            params = SingleStakingBalanceProducer.Params(
                userWalletId = userWalletId,
                stakingId = stakingId,
            ),
        )

        ensureNotNull(yieldBalance) { Error.EmptyStakingBalances }
    }

    private fun getIds(currencies: List<CryptoCurrency>): Pair<NonEmptySet<Network>, NonEmptySet<CryptoCurrency.ID>> {
        val currencyIdToNetworkId = currencies.associate { currency ->
            currency.id to currency.network
        }
        val currenciesIds = currencyIdToNetworkId.keys.toNonEmptySetOrNull()
        val networks = currencyIdToNetworkId.values.toNonEmptySetOrNull()

        requireNotNull(currenciesIds) { "Currencies IDs cannot be empty" }
        requireNotNull(networks) { "Networks IDs cannot be empty" }

        return networks to currenciesIds
    }
}