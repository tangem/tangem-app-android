package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * A use case for adding multiple cryptocurrencies to a user's wallet.
 *
 * This use case interacts with the underlying repositories to both add currencies and refresh
 * network statuses, particularly after the addition of new tokens.
 */
// TODO: Add tests
class AddCryptoCurrenciesUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val quotesRepository: QuotesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

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
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency): Either<Throwable, Unit> {
        return invoke(userWalletId, listOf(currency))
    }

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
        invoke(userWalletId = userWalletId, currencies = listOf(tokenToAdd))
    }

    /**
     * Adds a list of [currencies] to the wallet identified by [userWalletId].
     *
     * After successfully adding currencies, it also refreshes the networks for tokens
     * that are being added and have corresponding coins in the existing currencies list.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currencies The list of cryptocurrencies to add.
     * @return Either an [Throwable] or [Unit] indicating the success of the operation.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> = either {
        val existingCurrencies = catch({ currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId) }) {
            raise(it)
        }
        val currenciesToAdd = currencies
            .filterNot(existingCurrencies::contains)
            .toNonEmptyListOrNull()
            ?: return@either

        addCurrencies(userWalletId, currenciesToAdd)
        refreshUpdatedNetworks(userWalletId, currenciesToAdd, existingCurrencies)
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
        addCurrencies(userWalletId, listOf(tokenToAdd))
        refreshUpdatedNetworks(userWalletId, listOf(tokenToAdd), existingCurrencies)
        refreshUpdatedYieldBalances(userWalletId, existingCurrencies)
        refreshUpdatedQuotes(existingCurrencies)
        tokenToAdd
    }

    /**
     * Refreshes the network statuses for tokens that have corresponding coins in the
     * [existingCurrencies] list.
     */
    private suspend fun Raise<Throwable>.refreshUpdatedNetworks(
        userWalletId: UserWalletId,
        currenciesToAdd: List<CryptoCurrency>,
        existingCurrencies: List<CryptoCurrency>,
    ) {
        val networksToUpdate = currenciesToAdd
            .asSequence()
            .filterIsInstance<CryptoCurrency.Token>()
            .filter { hasCoinForToken(existingCurrencies, it) }
            .mapTo(hashSetOf(), CryptoCurrency.Token::network)

        val networkToUpdate = currenciesToAdd.map { it.network }
            .subtract(existingCurrencies.map { it.network }.toSet())

        if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            multiNetworkStatusFetcher(
                MultiNetworkStatusFetcher.Params(
                    userWalletId = userWalletId,
                    networks = networksToUpdate + networkToUpdate,
                ),
            )
        } else {
            catch(
                {
                    networksRepository.getNetworkStatusesSync(
                        userWalletId = userWalletId,
                        networks = networksToUpdate + networkToUpdate,
                        refresh = true,
                    )
                },
            ) {
                raise(it)
            }
        }
    }

    private suspend fun refreshUpdatedYieldBalances(
        userWalletId: UserWalletId,
        existingCurrencies: List<CryptoCurrency>,
    ) {
        stakingRepository.fetchMultiYieldBalance(
            userWalletId = userWalletId,
            cryptoCurrencies = existingCurrencies,
            refresh = true,
        )
    }

    private suspend fun refreshUpdatedQuotes(addedCurrencies: List<CryptoCurrency>) {
        quotesRepository.fetchQuotes(
            currenciesIds = addedCurrencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
            refresh = true,
        )
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

    private suspend fun Raise<Throwable>.addCurrencies(userWalletId: UserWalletId, tokens: List<CryptoCurrency>) {
        catch(
            { currenciesRepository.addCurrencies(userWalletId, tokens) },
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