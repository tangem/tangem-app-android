package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import kotlinx.collections.immutable.persistentListOf

internal object PredictionFeeSheetUMConverter {

    fun convert(quote: PredictionOrderQuote, slippage: SlippageUM, tokenSymbol: String): PredictionFeeSheetUM {
        return PredictionFeeSheetUM(
            rows = persistentListOf(
                PredictionFeeSheetUM.Row(
                    title = resourceReference(R.string.prediction_fee_sheet_order),
                    amount = quote.notional.formatPolymarketMoney(),
                    description = quote.orderDescription(slippage, tokenSymbol),
                ),
                PredictionFeeSheetUM.Row(
                    title = resourceReference(R.string.prediction_fee_sheet_polymarket_fee),
                    amount = quote.fees.market.formatPolymarketMoney(),
                ),
                PredictionFeeSheetUM.Row(
                    title = resourceReference(R.string.prediction_fee_sheet_builder_fee),
                    amount = quote.fees.builder.formatPolymarketMoney(),
                ),
                PredictionFeeSheetUM.Row(
                    title = resourceReference(R.string.prediction_fee_sheet_total),
                    amount = quote.total.formatPolymarketMoney(),
                ),
            ),
        )
    }

    private fun PredictionOrderQuote.orderDescription(slippage: SlippageUM, tokenSymbol: String): TextReference {
        val (descriptionRes, tradedShares) = when (side) {
            PredictionOrderSide.BUY -> R.string.prediction_fee_sheet_buy_description to expectedExecutionAmount
            PredictionOrderSide.SELL -> R.string.prediction_fee_sheet_sell_description to shares
        }
        return resourceReference(
            id = descriptionRes,
            formatArgs = wrappedList(
                tradedShares.formatPolymarketShares(),
                averagePrice.movePointRight(2).format { simple(decimals = 2) },
                tokenSymbol,
                slippage.percent.formatSlippagePercent(),
            ),
        )
    }
}