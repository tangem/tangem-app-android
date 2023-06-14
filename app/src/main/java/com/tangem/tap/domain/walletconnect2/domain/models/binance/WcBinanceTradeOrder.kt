package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
class WcBinanceTradeOrder(
    @Json(name = "account_number")
    accountNumber: String,
    @Json(name = "chain_id")
    chainId: String,
    data: String?,
    memo: String?,
    sequence: String,
    source: String,
    msgs: List<Message>,
) : WcBinanceOrder<WcBinanceTradeOrder.Message>(accountNumber, chainId, data, memo, sequence, source, msgs) {

    enum class MessageKey(val key: String) {
        ID("id"),
        ORDER_TYPE("ordertype"),
        PRICE("price"),
        QUANTITY("quantity"),
        SENDER("sender"),
        SIDE("side"),
        SYMBOL("symbol"),
        TIME_INFORCE("timeinforce"),
    }

    data class Message(
        val id: String,
        val orderType: Int,
        val price: Long,
        val quantity: Long,
        val sender: String,
        val side: Int,
        val symbol: String,
        val timeInforce: Int,
    )
}

val tradeOrderDeserializer = jsonDeserializer {
    WcBinanceTradeOrder.Message(
        id = it.json[WcBinanceTradeOrder.MessageKey.ID.key].string,
        orderType = it.json[WcBinanceTradeOrder.MessageKey.ORDER_TYPE.key].int,
        price = it.json[WcBinanceTradeOrder.MessageKey.PRICE.key].long,
        quantity = it.json[WcBinanceTradeOrder.MessageKey.QUANTITY.key].long,
        sender = it.json[WcBinanceTradeOrder.MessageKey.SENDER.key].string,
        side = it.json[WcBinanceTradeOrder.MessageKey.SIDE.key].int,
        symbol = it.json[WcBinanceTradeOrder.MessageKey.SYMBOL.key].string,
        timeInforce = it.json[WcBinanceTradeOrder.MessageKey.TIME_INFORCE.key].int,
    )
}

val tradeOrderSerializer = jsonSerializer<WcBinanceTradeOrder.Message> {
    val jsonObject = JsonObject()
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.ID.key, it.src.id)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.ORDER_TYPE.key, it.src.orderType)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.PRICE.key, it.src.price)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.QUANTITY.key, it.src.quantity)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.SENDER.key, it.src.sender)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.SIDE.key, it.src.side)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.SYMBOL.key, it.src.symbol)
    jsonObject.addProperty(WcBinanceTradeOrder.MessageKey.TIME_INFORCE.key, it.src.timeInforce)

    jsonObject
}