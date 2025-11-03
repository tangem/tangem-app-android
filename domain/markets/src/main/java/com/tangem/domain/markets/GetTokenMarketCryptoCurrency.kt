package com.tangem.domain.markets

import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

class GetTokenMarketCryptoCurrency(
    private val marketsTokenRepository: MarketsTokenRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tokenMarketParams: TokenMarketParams,
        network: TokenMarketInfo.Network,
    ): CryptoCurrency? {
        return marketsTokenRepository.createCryptoCurrency(
            userWalletId = userWalletId,
            token = tokenMarketParams,
            network = network,
        )
    }
}