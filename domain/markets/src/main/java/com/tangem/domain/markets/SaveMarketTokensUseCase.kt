package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
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
class SaveMarketTokensUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val quotesRepository: QuotesRepository,
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
                networkIds = addedNetworks.map { Network.ID(it.networkId) },
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
        networksRepository.getNetworkStatusesSync(
            userWalletId = userWalletId,
            networks = addedCurrencies.map(CryptoCurrency::network).toSet(),
            refresh = true,
        )
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
}