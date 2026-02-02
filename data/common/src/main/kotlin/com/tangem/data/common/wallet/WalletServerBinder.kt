package com.tangem.data.common.wallet

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

/**

 *
[REDACTED_AUTHOR]
 */
interface WalletServerBinder {

    /**
     * Binds a wallet to the server using its [userWalletId].
     * Returns an [ApiResponse] indicating the result of the binding operation,
     * or null if the binding was not performed.
     */
    suspend fun bind(userWalletId: UserWalletId): ApiResponse<Unit>?

    /**
     * Binds a wallet to the server using the provided [userWallet].
     * Returns an [ApiResponse] indicating the result of the binding operation.
     */
    suspend fun bind(userWallet: UserWallet): ApiResponse<Unit>
}