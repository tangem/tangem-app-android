package com.tangem.domain.txhistory.repository

import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/** Resolves locally cached express-asset token metadata (name/symbol/decimals/icon) without a network fetch. */
interface TokenInfoRepository {

    /**
     * Resolves a [CryptoCurrency.Token] for every id among [ids] that is already cached locally — no network
     * fetch. Ids with no cache entry are simply absent from the result map.
     */
    suspend fun getCachedTokens(
        userWalletId: UserWalletId,
        ids: Set<ExpressAsset.ID>,
    ): Map<ExpressAsset.ID, CryptoCurrency.Token>

    /**
     * Every [CryptoCurrency.Token] cached for [networkId] — the full known set, not filtered to particular
     * contracts. For a caller that doesn't know in advance which contracts it needs (e.g. resolving an ERC-20
     * approve's target, which may be any token on the network).
     */
    suspend fun getCachedTokensForNetwork(userWalletId: UserWalletId, networkId: String): Set<CryptoCurrency.Token>

    fun fetchMissing(ids: Set<ExpressAsset.ID>)

    /** Writes through the metadata of known (portfolio) tokens, so it's cached even after a token is removed. */
    suspend fun save(currencies: Collection<CryptoCurrency>)
}