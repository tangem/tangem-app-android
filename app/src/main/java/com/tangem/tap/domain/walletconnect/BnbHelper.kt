package com.tangem.tap.domain.walletconnect

import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha256
import com.tangem.tap.features.details.redux.walletconnect.BinanceMessageData
import com.tangem.tap.features.details.redux.walletconnect.TradeData
import com.trustwallet.walletconnect.models.binance.WCBinanceTradeOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTransferOrder
import com.trustwallet.walletconnect.models.binance.tradeOrderSerializer
import timber.log.Timber

class BnbHelper {
    companion object {

        fun createMessageData(order: WCBinanceTransferOrder): BinanceMessageData.Transfer {
            val input = order.msgs.first().inputs.first()
            val output = order.msgs.first().inputs.first()

            val currency =
                input.coins.firstOrNull()?.denom ?: Blockchain.Binance
            val amount = input.coins
                .mapNotNull { if (it.denom == currency) it.amount else null }
                .sum()
                .toBigDecimal()
                .movePointRight(Blockchain.Binance.decimals())
                .toString()

            val gson = GsonBuilder()
                .registerTypeAdapter(tradeOrderSerializer)
                .serializeNulls()
                .create()

            return BinanceMessageData.Transfer(
                outputAddress = output.address,
                amount = amount,
                address = input.address,
                data = gson.toJson(order).toByteArray().calculateSha256()
            )
        }

        fun createMessageData(order: WCBinanceTradeOrder): BinanceMessageData.Trade {
            val address = order.msgs.first().sender

            val tradeData = order.msgs.map {
                val price = it.price.toBigDecimal()
                    .movePointLeft(Blockchain.Binance.decimals())
                    .stripTrailingZeros()
                val quantity = it.quantity.toBigDecimal()
                    .movePointLeft(Blockchain.Binance.decimals())
                    .stripTrailingZeros()

                val amount = price * quantity
                val symbol = it.symbol.substringBefore("-")

                TradeData(
                    price = "$price ${Blockchain.Binance.currency}",
                    quantity = "$quantity $symbol",
                    amount = "$amount ${Blockchain.Binance.currency}",
                    symbol = symbol
                )
            }
            val gson = GsonBuilder()
                .registerTypeAdapter(tradeOrderSerializer)
                .serializeNulls()
                .create()
            val serialized = gson.toJson(order)
            Timber.d(serialized)

            return BinanceMessageData.Trade(
                tradeData = tradeData,
                address = address,
                data = serialized.toByteArray().calculateSha256()
            )
        }

    }
}
