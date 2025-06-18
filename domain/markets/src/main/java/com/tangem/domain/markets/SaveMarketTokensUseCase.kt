package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for saving tokens from Markets
 *
 * @property derivationsRepository  derivations repository
 * @property marketsTokenRepository markets token repository
 * @property currenciesRepository   currencies repository
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class SaveMarketTokensUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val stakingRepository: StakingRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tokenMarketParams: TokenMarketParams,
        addedNetworks: Set<TokenMarketInfo.Network>,
        removedNetworks: Set<TokenMarketInfo.Network>,
    ): Either<Throwable, Unit> = Either.catch {
        if (removedNetworks.isNotEmpty()) {
            currenciesRepository.removeCurrencies(
                userWalletId = userWalletId,
                currencies = removedNetworks.mapNotNull {
                    marketsTokenRepository.createCryptoCurrency(
                        userWalletId = userWalletId,
                        token = tokenMarketParams,
                        network = it,
                    )
                },
            )
        }

        if (addedNetworks.isNotEmpty()) {
            derivationsRepository.derivePublicKeysByNetworkIds(
                userWalletId = userWalletId,
                networkIds = addedNetworks.map { Network.RawID(it.networkId) },
            )

            val addedCurrencies = addedNetworks.mapNotNull {
                marketsTokenRepository.createCryptoCurrency(
                    userWalletId = userWalletId,
                    token = tokenMarketParams,
                    network = it,
                )
            }

            currenciesRepository.addCurrencies(userWalletId = userWalletId, currencies = addedCurrencies)

            refreshUpdatedNetworks(userWalletId, addedCurrencies)

            refreshUpdatedYieldBalances(userWalletId, addedCurrencies)

            refreshUpdatedQuotes(addedCurrencies)
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
        if (tokensFeatureToggles.isStakingLoadingRefactoringEnabled) {
            multiYieldBalanceFetcher(
                params = MultiYieldBalanceFetcher.Params(
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
        multiQuoteFetcher(
            params = MultiQuoteFetcher.Params(
                currenciesIds = addedCurrencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                appCurrencyId = null,
            ),
        )
    }
}