package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.flatten
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

@Suppress("LongParameterList")
class SaveManagedTokensUseCase(
    private val customTokensRepository: CustomTokensRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val derivationsRepository: DerivationsRepository,
    private val stakingRepository: StakingRepository,
    private val quotesRepository: QuotesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> = Either.catch {
        val removingCurrencies = currenciesToRemove.mapToCryptoCurrencies(userWalletId)
        val addingCurrencies = currenciesToAdd.mapToCryptoCurrencies(userWalletId)

        derivationsRepository.derivePublicKeysByNetworks(
            userWalletId = userWalletId,
            networks = currenciesToAdd.values.flatten(),
        )
        val newCurrenciesList = currenciesRepository
            .getMultiCurrencyWalletCurrenciesSync(userWalletId)
            .filterNot(removingCurrencies::contains)
            .toMutableList()
            .also { it.addAll(addingCurrencies) }

        currenciesRepository.saveNewCurrenciesList(userWalletId, newCurrenciesList)

        val existingCurrencies = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        removeCurrenciesFromWalletManager(
            userWalletId = userWalletId,
            currencies = removingCurrencies.filterNot(existingCurrencies::contains),
        )
        refreshUpdatedNetworks(
            userWalletId = userWalletId,
            existingCurrencies = existingCurrencies,
            currenciesToAdd = addingCurrencies,
        )

        refreshUpdatedYieldBalances(userWalletId, existingCurrencies)

        refreshUpdatedQuotes(addingCurrencies)
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

    private suspend fun refreshUpdatedNetworks(
        userWalletId: UserWalletId,
        currenciesToAdd: List<CryptoCurrency>,
        existingCurrencies: List<CryptoCurrency>,
    ) {
        val networksToUpdate = currenciesToAdd
            .asSequence()
            .filterIsInstance<CryptoCurrency.Token>()
            .map(CryptoCurrency.Token::network)
            .filterTo(hashSetOf()) { hasCoinForNetwork(existingCurrencies, it) }

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
            networksRepository.getNetworkStatusesSync(
                userWalletId = userWalletId,
                networks = networksToUpdate + networkToUpdate,
                refresh = true,
            )
        }
    }

    private suspend fun refreshUpdatedYieldBalances(
        userWalletId: UserWalletId,
        existingCurrencies: List<CryptoCurrency>,
    ) {
        if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            multiYieldBalanceFetcher(
                params = YieldBalanceFetcherParams.Multi(
                    userWalletId = userWalletId,
                    currencyIdWithNetworkMap = existingCurrencies.associateTo(hashMapOf()) { it.id to it.network },
                ),
            )
        } else {
            stakingRepository.fetchMultiYieldBalance(
                userWalletId = userWalletId,
                cryptoCurrencies = existingCurrencies,
                refresh = true,
            )
        }
    }

    private suspend fun refreshUpdatedQuotes(addedCurrencies: List<CryptoCurrency>) {
        if (tokensFeatureToggles.isQuotesLoadingRefactoringEnabled) {
            multiQuoteFetcher(
                params = MultiQuoteFetcher.Params(
                    currenciesIds = addedCurrencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                    appCurrencyId = null,
                ),
            )
        } else {
            quotesRepository.fetchQuotes(
                currenciesIds = addedCurrencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                refresh = true,
            )
        }
    }

    /**
     * Determines if the [existingCurrencies] list contains a coin that corresponds
     * to the given [network].
     */
    private fun hasCoinForNetwork(existingCurrencies: List<CryptoCurrency>, network: Network): Boolean {
        return existingCurrencies.any { currency ->
            currency is CryptoCurrency.Coin && currency.network == network
        }
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