package com.tangem.domain.markets

import arrow.core.Either
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Use case for saving tokens from Markets
 *
 * @property derivationsRepository  derivations repository
 * @property marketsTokenRepository markets token repository
 * @property currenciesRepository   currencies repository
 *
 * @author Andrew Khokhlov on 02/09/2024
 */
class SaveMarketTokensUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tokenMarketParams: TokenMarketParams,
        addedNetworks: Set<TokenMarketInfo.Network>,
        removedNetworks: Set<TokenMarketInfo.Network>,
    ): Either<Throwable, Unit> = Either.catch {
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

        derivationsRepository.derivePublicKeysByNetworkIds(
            userWalletId = userWalletId,
            networkIds = addedNetworks.map { Network.ID(it.networkId) },
        )

        currenciesRepository.addCurrencies(
            userWalletId = userWalletId,
            currencies = addedNetworks.mapNotNull {
                marketsTokenRepository.createCryptoCurrency(
                    userWalletId = userWalletId,
                    token = tokenMarketParams,
                    network = it,
                )
            },
        )

        // TODO: https://tangem.atlassian.net/browse/AND-8214
    }
}
