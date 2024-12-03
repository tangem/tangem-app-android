package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.flatten
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId

class SaveManagedTokensUseCase(
    private val customTokensRepository: CustomTokensRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val derivationsRepository: DerivationsRepository,
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

        networksRepository.getNetworkStatusesSync(
            userWalletId = userWalletId,
            networks = networksToUpdate + networkToUpdate,
            refresh = true,
        )
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
                            rawId = token.id.value,
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
