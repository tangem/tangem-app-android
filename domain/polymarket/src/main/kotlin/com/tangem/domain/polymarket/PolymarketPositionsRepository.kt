package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsPageRequest
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import java.math.BigDecimal

/** Read-only portfolio. Every request carries the Tangem wallet id and its deposit-wallet address. */
interface PolymarketPositionsRepository {

    /** Each invocation owns its pagination state, independently of other wallets or tabs. */
    fun getPositionsBatchFlow(context: PolymarketPositionsBatchingContext, batchSize: Int): PolymarketPositionsBatchFlow

    /** Open holdings, most valuable first. The first page takes a limit of 1..30. */
    suspend fun getOpenPositions(
        addresses: PolymarketAddresses,
        request: PolymarketPositionsPageRequest,
    ): Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition.Open>>

    /** History, most recently closed first. Paged like [getOpenPositions]. */
    suspend fun getClosedPositions(
        addresses: PolymarketAddresses,
        request: PolymarketPositionsPageRequest,
    ): Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition.Closed>>

    /** A successful null is absence (or indexing delay after an order), not a transport failure. */
    suspend fun getPosition(
        addresses: PolymarketAddresses,
        id: PolymarketPositionId,
    ): Either<PolymarketPositionsError, PolymarketPosition.Open?>

    /** The current value of every open holding, read from the data API without a wallet id. Zero when there are none. */
    suspend fun getPositionsValue(addresses: PolymarketAddresses): Either<PolymarketPositionsError, BigDecimal>
}