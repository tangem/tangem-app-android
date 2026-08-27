package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteFeesResponse
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteRequest
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteResponse
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal

internal object PredictionOrderQuoteConverter : Converter<PolymarketOrderQuoteResponse, PredictionOrderQuote> {

    private val logger = TangemLogger.withTag(tag = "PredictionOrderQuoteConverter")

    override fun convert(value: PolymarketOrderQuoteResponse): PredictionOrderQuote = PredictionOrderQuote(
        status = value.status.toStatus(),
        side = value.side.toSide(),
        shares = BigDecimal(value.shares),
        notional = BigDecimal(value.notional),
        expectedExecutionAmount = BigDecimal(value.expectedExecutionAmount),
        averagePrice = BigDecimal(value.averagePrice),
        worstCasePrice = BigDecimal(value.worstCasePrice),
        fees = value.fees.toFees(),
        total = BigDecimal(value.total),
        builderCode = value.builderCode,
        minOrderSize = BigDecimal(value.minOrderSize),
        tickSize = BigDecimal(value.tickSize),
        isLive = value.isLive,
    )

    fun toRequestBody(request: PredictionOrderQuoteRequest): PolymarketOrderQuoteRequest = PolymarketOrderQuoteRequest(
        marketId = request.marketId,
        assetId = request.assetId,
        side = request.side.name,
        amount = request.amount.toPlainString(),
        slippage = request.slippagePercent?.toPlainString(),
    )

    /**
     * Fail-closed: a status this build does not know is treated as unplaceable, not as a full fill. A new
     * status the BFF introduces will read as a market we cannot trade rather than as permission to trade.
     */
    private fun String.toStatus(): PredictionQuoteStatus = PredictionQuoteStatus.entries
        .firstOrNull { it.name == this }
        ?: PredictionQuoteStatus.MARKET_CLOSED.also {
            logger.e("Unknown quote status '$this': this quote reads as a closed market")
        }

    /** A side the backend echoes back that this build cannot read is not something to guess at. */
    private fun String.toSide(): PredictionOrderSide = PredictionOrderSide.entries
        .firstOrNull { it.name == this }
        ?: error("Unknown order side: $this")

    private fun PolymarketOrderQuoteFeesResponse.toFees(): PredictionOrderFees = PredictionOrderFees(
        market = BigDecimal(market),
        builder = BigDecimal(builder),
        total = BigDecimal(total),
    )
}