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
    ): Either<Throwable, Unit> {
        return Either.catch {
            // TODO: Currently order is matter. [REDACTED_JIRA]
            removeCurrencies(userWalletId, currenciesToRemove)
            addCurrencies(userWalletId, currenciesToAdd)
        }
    }

    private suspend fun removeCurrencies(
        userWalletId: UserWalletId,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ) {
        if (currenciesToRemove.isEmpty()) return

        val currencies = currenciesToRemove.mapToCryptoCurrencies()
        currenciesRepository.removeCurrencies(userWalletId = userWalletId, currencies = currencies)

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

    private suspend fun addCurrencies(
        userWalletId: UserWalletId,
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ) {
        if (currenciesToAdd.isEmpty()) return

        val existingCurrencies = currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId)
        val currencies = currenciesToAdd.mapToCryptoCurrencies()
        derivationsRepository.derivePublicKeysByNetworks(
            userWalletId = userWalletId,
            networks = currenciesToAdd.values.flatten(),
        )
        currenciesRepository.addCurrencies(userWalletId, currencies)
        refreshUpdatedNetworks(
            userWalletId = userWalletId,
            existingCurrencies = existingCurrencies,
            currenciesToAdd = currencies,
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

    private fun Map<ManagedCryptoCurrency.Token, Set<Network>>.mapToCryptoCurrencies(): List<CryptoCurrency> {
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
                            networkId = sourceNetwork.id,
                            derivationPath = sourceNetwork.network.derivationPath,
                        )
                    }
                }
        }
    }
}