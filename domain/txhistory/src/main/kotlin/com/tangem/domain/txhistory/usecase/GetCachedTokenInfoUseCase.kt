package com.tangem.domain.txhistory.usecase

import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository

/** Resolves a [CryptoCurrency.Token] for every [ExpressAsset.ID] that is already cached locally — no network fetch. */
class GetCachedTokenInfoUseCase(private val repository: TokenInfoRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        ids: Set<ExpressAsset.ID>,
    ): Map<ExpressAsset.ID, CryptoCurrency.Token> = repository.getCachedTokens(userWalletId, ids)

    suspend operator fun invoke(userWalletId: UserWalletId, id: ExpressAsset.ID): CryptoCurrency.Token? =
        invoke(userWalletId, setOf(id)).values.firstOrNull()

    suspend operator fun invoke(userWalletId: UserWalletId, networkId: String): Set<CryptoCurrency.Token> =
        repository.getCachedTokensForNetwork(userWalletId, networkId)

    fun fetchMissing(ids: Set<ExpressAsset.ID>) = repository.fetchMissing(ids)
}