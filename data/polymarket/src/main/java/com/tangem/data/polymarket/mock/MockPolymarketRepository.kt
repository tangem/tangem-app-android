package com.tangem.data.polymarket.mock

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import javax.inject.Inject

/**
 * UI-first [PolymarketRepository] that serves the events feed from static fixtures ([PolymarketMockData]) while
 * that BFF endpoint is not deployed yet.
 *
 * The wallet endpoints are already backed by the real BFF, so [com.tangem.data.polymarket.DefaultPolymarketRepository]
 * owns them — this mock only stands in for [getEvents], and its wallet overrides fail loudly should the whole
 * repository ever be swapped for the mock while a wallet flow is live.
 */
internal class MockPolymarketRepository @Inject constructor() : PolymarketRepository {

    override suspend fun getEvents(): Either<DataError, List<PolymarketEvent>> = PolymarketMockData.events.right()

    override suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState> =
        PolymarketWalletError.InvalidRequest.left()

    override suspend fun deployWallet(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletStatus> =
        PolymarketWalletError.InvalidRequest.left()

    override suspend fun submitApprovals(
        batch: PolymarketApprovalsBatch,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = PolymarketWalletError.InvalidRequest.left()
}