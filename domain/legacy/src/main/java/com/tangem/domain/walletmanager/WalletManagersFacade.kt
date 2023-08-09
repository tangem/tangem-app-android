package com.tangem.domain.walletmanager

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
// [REDACTED_TODO_COMMENT]
/**
 * A facade for managing wallets.
 */
interface WalletManagersFacade {

    /**
     * Updates the wallet manager associated with a user's wallet and network.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networkId The network ID.
     * @param extraTokens Additional tokens.
     * @return The result of updating the wallet manager.
     */
    suspend fun update(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        extraTokens: Set<CryptoCurrency.Token>,
    ): UpdateWalletManagerResult

    suspend fun getExploreUrl(userWalletId: UserWalletId, networkId: Network.ID): String

    /**
     * Returns transactions count
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networkId The network ID.
     * @param rawDerivationPath Derivation path in raw form.

     */
    suspend fun getTxHistoryState(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        rawDerivationPath: String?,
    ): TxHistoryState

    /**
     * Returns transaction history items wrapped to pagination
     *
     * @param userWalletId The ID of the user's wallet.
     * @param networkId The network ID.
     * @param rawDerivationPath Derivation path in raw form.
     * @param page Pagination page.
     * @param pageSize Pagination size.
     */
    suspend fun getTxHistoryItems(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        rawDerivationPath: String?,
        page: Int,
        pageSize: Int,
    ): PaginationWrapper<TxHistoryItem>
}
