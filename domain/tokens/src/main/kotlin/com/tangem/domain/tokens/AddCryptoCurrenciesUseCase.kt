package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * A use case for adding multiple cryptocurrencies to a user's wallet.
 *
 * This use case interacts with the underlying repositories to both add currencies and refresh
 * network statuses, particularly after the addition of new tokens.
 */
// TODO: Add tests
@Suppress("LongParameterList")
class AddCryptoCurrenciesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val stakingRepository: StakingRepository,
    private val quotesRepository: QuotesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    /**
     * Adds a [cryptoCurrency] token with specific [network] and derivation to the wallet identified by [userWalletId].
     *
     * After successfully adding a currency, it also refreshes the networks for tokens
     * that are being added and have corresponding coins in the existing currencies list.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param cryptoCurrency Token to add.
     * @param network Network where we add
     * @return Either an [Throwable] or [Unit] indicating the success of the operation.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        network: Network,
    ): Either<Throwable, Unit> = either {
        val tokenToAdd = currenciesRepository.createTokenCurrency(cryptoCurrency = cryptoCurrency, network = network)
        invoke(userWalletId = userWalletId, currency = tokenToAdd)
    }

    /**
     * Adds a [currency] to the wallet identified by [userWalletId].
     *
     * After successfully adding a currency, it also refreshes the networks for tokens
     * that are being added and have corresponding coins in the existing currencies list.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currency Cryptocurrency to add.
     * @return Either an [Throwable] or [Unit] indicating the success of the operation.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency): Either<Throwable, Unit> =
        either {
            val existingCurrencies = catch(
                block = { currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId) },
                catch = ::raise,
            )
            val currencyToAdd = currency.takeUnless(existingCurrencies::contains) ?: return@either

            addCurrencies(userWalletId, currencyToAdd)

            coroutineScope {
                awaitAll(
                    async { refreshUpdatedNetworks(userWalletId, currencyToAdd, existingCurrencies) },
                    async { refreshUpdatedYieldBalances(userWalletId, currencyToAdd) },
                    async { refreshUpdatedQuotes(currencyToAdd) },
                )
            }
        }

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): Either<Throwable, CryptoCurrency> = either {
        val existingCurrencies =
            catch({ currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId) }) {
                raise(it)
            }
        val foundToken = existingCurrencies
            .filterIsInstance<CryptoCurrency.Token>()
            .firstOrNull {
                it.network.backendId == networkId &&
                    !it.isCustom &&
                    it.contractAddress.equals(contractAddress, true)
            }
        if (foundToken != null) {
            return@either foundToken
        }
        val tokenToAdd = createTokenCurrency(userWalletId, contractAddress, networkId)
        addCurrencies(userWalletId, tokenToAdd)

        coroutineScope {
            awaitAll(
                async { refreshUpdatedNetworks(userWalletId, tokenToAdd, existingCurrencies) },
                async { refreshUpdatedYieldBalances(userWalletId, tokenToAdd) },
                async { refreshUpdatedQuotes(tokenToAdd) },
            )
        }

        tokenToAdd
    }

    /**
     * Refreshes the network statuses for tokens that have corresponding coins in the
     * [existingCurrencies] list.
     */
    private suspend fun Raise<Throwable>.refreshUpdatedNetworks(
        userWalletId: UserWalletId,
        currencyToAdd: CryptoCurrency,
        existingCurrencies: List<CryptoCurrency>,
    ) {
        val networksToUpdate = currencyToAdd.takeIf { currency ->
            currency is CryptoCurrency.Token && hasCoinForToken(existingCurrencies, currency)
        }
            ?.network

        val networkToUpdate = currencyToAdd.takeIf {
            !existingCurrencies.map(CryptoCurrency::network).contains(it.network)
        }
            ?.network

        multiNetworkStatusFetcher(
            MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = setOfNotNull(networksToUpdate, networkToUpdate),
            ),
        )
            .bind()
    }

    private suspend fun refreshUpdatedYieldBalances(userWalletId: UserWalletId, addedCurrency: CryptoCurrency) {
        if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            singleYieldBalanceFetcher(
                params = YieldBalanceFetcherParams.Single(
                    userWalletId = userWalletId,
                    currencyId = addedCurrency.id,
                    network = addedCurrency.network,
                ),
            )
        } else {
            stakingRepository.fetchSingleYieldBalance(
                userWalletId = userWalletId,
                cryptoCurrency = addedCurrency,
                refresh = true,
            )
        }
    }

    private suspend fun refreshUpdatedQuotes(currencyToAdd: CryptoCurrency) {
        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            multiQuoteFetcher(
                params = MultiQuoteFetcher.Params(
                    currenciesIds = setOfNotNull(currencyToAdd.id.rawCurrencyId),
                    appCurrencyId = null,
                ),
            )
        } else {
            quotesRepository.fetchQuotes(
                currenciesIds = setOfNotNull(currencyToAdd.id.rawCurrencyId),
                refresh = true,
            )
        }
    }

    private suspend fun Raise<Throwable>.createTokenCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): CryptoCurrency.Token {
        return catch(
            block = {
                currenciesRepository.createTokenCurrency(
                    userWalletId = userWalletId,
                    contractAddress = contractAddress,
                    networkId = networkId,
                )
            },
            catch = {
                raise(it)
            },
        )
    }

    private suspend fun Raise<Throwable>.addCurrencies(userWalletId: UserWalletId, currency: CryptoCurrency) {
        catch(
            { currenciesRepository.addCurrencies(userWalletId, listOf(currency)) },
        ) {
            raise(it)
        }
    }

    /**
     * Determines if the [existingCurrencies] list contains a coin that corresponds
     * to the given [token].
     */
    private fun hasCoinForToken(existingCurrencies: List<CryptoCurrency>, token: CryptoCurrency.Token): Boolean {
        return existingCurrencies.any { currency ->
            currency is CryptoCurrency.Coin && currency.network == token.network
        }
    }
}