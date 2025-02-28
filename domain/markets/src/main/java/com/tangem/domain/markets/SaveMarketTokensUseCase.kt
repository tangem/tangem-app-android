package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
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

            networksRepository.getNetworkStatusesSync(
                userWalletId = userWalletId,
                networks = addedCurrencies.map(CryptoCurrency::network).toSet(),
                refresh = true,
            )
        }
    }
}