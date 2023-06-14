package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
class WcBinanceTransferOrder(
    @Json(name = "account_number")
    accountNumber: String,
    @Json(name = "chain_id")
    chainId: String,
    data: String?,
    memo: String?,
    sequence: String,
    source: String,
    msgs: List<Message>,
) : WcBinanceOrder<WcBinanceTransferOrder.Message>(accountNumber, chainId, data, memo, sequence, source, msgs) {

    enum class MessageKey(val key: String) {
        INPUTS("inputs"),
        OUTPUTS("outputs"),
    }

    data class Message(
        val inputs: List<Item>,
        val outputs: List<Item>,
    ) {

        data class Item(
            val address: String,
            val coins: List<Coin>,
        ) {

            data class Coin(
                val amount: Long,
                val denom: String,
            )
        }
    }
}