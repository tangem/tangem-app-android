package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.flatten
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

@Suppress("LongParameterList")
class SaveManagedTokensUseCase(
    private val customTokensRepository: CustomTokensRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val derivationsRepository: DerivationsRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
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

            refreshUpdatedNetworks(userWalletId = userWalletId, addedCurrencies = savedCurrencies)

            refreshUpdatedYieldBalances(userWalletId = userWalletId, addedCurrencies = savedCurrencies)

            refreshUpdatedQuotes(addedCurrencies = savedCurrencies)
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

    private suspend fun refreshUpdatedNetworks(userWalletId: UserWalletId, addedCurrencies: List<CryptoCurrency>) {
        multiNetworkStatusFetcher(
            MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = addedCurrencies.map(CryptoCurrency::network).toSet(),
            ),
        )

        currenciesRepository.syncTokens(userWalletId)
    }

    private suspend fun refreshUpdatedYieldBalances(
        userWalletId: UserWalletId,
        addedCurrencies: List<CryptoCurrency>,
    ) {
        multiYieldBalanceFetcher(
            params = MultiYieldBalanceFetcher.Params(
                userWalletId = userWalletId,
                currencyIdWithNetworkMap = addedCurrencies.associateTo(hashMapOf()) { it.id to it.network },
            ),
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