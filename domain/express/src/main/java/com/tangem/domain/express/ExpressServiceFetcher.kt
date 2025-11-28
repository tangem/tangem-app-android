package com.tangem.domain.express

import arrow.core.Either
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Service interface to fetch Express assets data and monitor initialization status.
 */
interface ExpressServiceFetcher {

    /**
     * Fetches Express assets data for the given user wallet ID and asset IDs.
     *
     * @param userWalletId The ID of the user wallet.
     * @param assetIds The list of Express asset IDs to fetch.
     */
    suspend fun fetch(userWalletId: UserWalletId, assetIds: Set<ExpressAsset.ID>): Either<Throwable, Unit>

    /**
     * Fetches Express assets data for the given user wallet and asset IDs.
     *
     * @param userWallet The user wallet for which to fetch assets.
     * @param assetIds The list of Express asset IDs to fetch.
     */
    suspend fun fetch(userWallet: UserWallet, assetIds: Set<ExpressAsset.ID>): Either<Throwable, Unit>

    /**
     * Returns a flow that emits the initialization status of Express assets for the given user wallet ID.
     *
     * @param userWalletId The ID of the user wallet.
     * @return A flow emitting Lce states containing either a list of Express assets or an error.
     */
    fun getInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, List<ExpressAsset>>>
}