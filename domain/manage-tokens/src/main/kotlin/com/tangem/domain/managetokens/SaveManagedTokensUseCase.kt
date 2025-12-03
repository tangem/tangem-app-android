package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.flatten
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Deprecated("Use ManageCryptoCurrenciesUseCase")
@Suppress("LongParameterList")
class SaveManagedTokensUseCase(
    private val customTokensRepository: CustomTokensRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val derivationsRepository: DerivationsRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val parallelUpdatingScope: CoroutineScope,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> = Either.catch {
        if (currenciesToRemove.isNotEmpty()) {
            val removingCurrencies = currenciesToRemove.mapToCryptoCurrencies(userWalletId)

            currenciesRepository.removeCurrencies(userWalletId = userWalletId, currencies = removingCurrencies)

            removeCurrenciesFromWalletManager(userWalletId = userWalletId, currencies = removingCurrencies)
        }

        if (currenciesToAdd.isNotEmpty()) {
            derivationsRepository.derivePublicKeysByNetworks(
                userWalletId = userWalletId,
                networks = currenciesToAdd.values.flatten(),
            )

            val addingCurrencies = currenciesToAdd.mapToCryptoCurrencies(userWalletId)

            val savedCurrencies = currenciesRepository.addCurrenciesCache(
                userWalletId = userWalletId,
                currencies = addingCurrencies,
            )

            parallelUpdatingScope.launch {
                withContext(NonCancellable) {
                    syncTokens(userWalletId = userWalletId, addedCurrencies = savedCurrencies)

                    launch {
                        refreshUpdatedNetworks(
                            userWalletId = userWalletId,
                            addedCurrencies = savedCurrencies,
                        )
                    }
                    launch {
                        refreshUpdatedYieldBalances(
                            userWalletId = userWalletId,
                            addedCurrencies = savedCurrencies,
                        )
                    }
                    launch { refreshUpdatedQuotes(addedCurrencies = savedCurrencies) }
                }
            }
        }
    }

    private suspend fun removeCurrenciesFromWalletManager(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ) {
        walletManagersFacade.remove(
            userWalletId = userWalletId,
            networks = currencies
                .filterIsInstance<CryptoCurrency.Coin>()
                .mapTo(hashSetOf(), CryptoCurrency::network),
        )

        walletManagersFacade.removeTokens(
            userWalletId = userWalletId,
            tokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )
    }

    private suspend fun syncTokens(userWalletId: UserWalletId, addedCurrencies: List<CryptoCurrency>) {
        createWalletManagers(userWalletId = userWalletId, currencies = addedCurrencies)
        currenciesRepository.syncTokens(userWalletId)
    }

    /**
     * Creates wallet managers for the given [currencies] if they do not already exist.
     * The method will generate addresses for new networks to ensure the stability of the "Push notifications" feature.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currencies The list of cryptocurrencies for which to create wallet managers.
     */
    private suspend fun createWalletManagers(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network)

        for (network in networks) {
            walletManagersFacade.getOrCreateWalletManager(userWalletId = userWalletId, network = network)
        }
    }

    private suspend fun refreshUpdatedNetworks(userWalletId: UserWalletId, addedCurrencies: List<CryptoCurrency>) {
        multiNetworkStatusFetcher(
            MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = addedCurrencies.map(CryptoCurrency::network).toSet(),
            ),
        )
    }

    private suspend fun refreshUpdatedYieldBalances(
        userWalletId: UserWalletId,
        addedCurrencies: List<CryptoCurrency>,
    ) {
        val stakingIds = addedCurrencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        multiYieldBalanceFetcher(
            params = MultiYieldBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }

    private suspend fun refreshUpdatedQuotes(addedCurrencies: List<CryptoCurrency>) {
        multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = addedCurrencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                appCurrencyId = null,
            ),
        )
    }

    private suspend fun Map<ManagedCryptoCurrency.Token, Set<Network>>.mapToCryptoCurrencies(
        userWalletId: UserWalletId,
    ): List<CryptoCurrency> {
        return flatMap { (token, networks) ->
            token.availableNetworks
                .filter { sourceNetwork -> networks.contains(sourceNetwork.network) }
                .map { sourceNetwork ->
                    when (sourceNetwork) {
                        is ManagedCryptoCurrency.SourceNetwork.Default -> customTokensRepository.createToken(
                            managedCryptoCurrency = token,
                            sourceNetwork = sourceNetwork,
                            rawId = CryptoCurrency.RawID(token.id.value),
                        )
                        is ManagedCryptoCurrency.SourceNetwork.Main -> customTokensRepository.createCoin(
                            userWalletId = userWalletId,
                            networkId = sourceNetwork.id,
                            derivationPath = sourceNetwork.network.derivationPath,
                        )
                    }
                }
        }
    }
}