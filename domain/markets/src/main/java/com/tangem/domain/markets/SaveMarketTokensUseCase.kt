package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.models.account.DerivationIndex
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

/**
 * Use case for saving tokens from Markets
 *
 * @property derivationsRepository  derivations repository
 * @property marketsTokenRepository markets token repository
 * @property currenciesRepository   currencies repository
 *
[REDACTED_AUTHOR]
 */
@Deprecated("Use ManageCryptoCurrenciesUseCase")
@Suppress("LongParameterList")
class SaveMarketTokensUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val parallelUpdatingScope: CoroutineScope,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tokenMarketParams: TokenMarketParams,
        addedNetworks: Set<TokenMarketInfo.Network>,
        removedNetworks: Set<TokenMarketInfo.Network>,
    ): Either<Throwable, Unit> = Either.catch {
        if (removedNetworks.isNotEmpty()) {
            val removedCurrencies = removedNetworks.mapNotNull { network ->
                marketsTokenRepository.createCryptoCurrency(
                    userWalletId = userWalletId,
                    token = tokenMarketParams,
                    network = network,
                )
            }

            currenciesRepository.removeCurrencies(userWalletId = userWalletId, currencies = removedCurrencies)
        }

        if (addedNetworks.isNotEmpty()) {
            derivationsRepository.derivePublicKeysByNetworkIds(
                userWalletId = userWalletId,
                networkIds = addedNetworks.map { Network.RawID(it.networkId) },
                accountIndex = DerivationIndex.Main,
            )

            val addedCurrencies = addedNetworks.mapNotNull { network ->
                marketsTokenRepository.createCryptoCurrency(
                    userWalletId = userWalletId,
                    token = tokenMarketParams,
                    network = network,
                    accountIndex = DerivationIndex.Main,
                )
            }

            val savedCurrencies = currenciesRepository.addCurrenciesCache(
                userWalletId = userWalletId,
                currencies = addedCurrencies,
            )

            parallelUpdatingScope.launch {
                withContext(NonCancellable) {
                    syncTokens(userWalletId, savedCurrencies)

                    launch { refreshUpdatedNetworks(userWalletId, savedCurrencies) }
                    launch { refreshUpdatedYieldBalances(userWalletId, savedCurrencies) }
                    launch { refreshUpdatedQuotes(savedCurrencies) }
                }
            }
        }
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
        existingCurrencies: List<CryptoCurrency>,
    ) {
        val stakingIds = existingCurrencies.mapNotNullTo(hashSetOf()) {
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
}