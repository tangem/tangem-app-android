package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.github.salomonbrys.kotson.jsonSerializer
import com.google.gson.JsonObject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
data class WcBinanceTradeOrder(
    @Json(name = "account_number")
    val accountNumber: String,
    @Json(name = "chain_id")
    val chainId: String,
    @Json(name = "data")
    val data: String?,
    @Json(name = "memo")
    val memo: String?,
    @Json(name = "sequence")
    val sequence: String,
    @Json(name = "source")
    val source: String,
    @Json(name = "msgs")
    val msgs: List<Message>,
) : WcRequestData {

    @JsonClass(generateAdapter = false)
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

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "id")
        val id: String,
        @Json(name = "orderType")
        val orderType: Int,
        @Json(name = "price")
        val price: Long,
        @Json(name = "quantity")
        val quantity: Long,
        @Json(name = "sender")
        val sender: String,
        @Json(name = "side")
        val side: Int,
        @Json(name = "symbol")
        val symbol: String,
        @Json(name = "timeInforce")
        val timeInforce: Int,
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