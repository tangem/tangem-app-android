package com.tangem.tap.domain.walletconnect2.domain.models.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@Suppress("LongParameterList")
@JsonClass(generateAdapter = true)
data class WcBinanceTransferOrder(
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

    @JsonClass(generateAdapter = true)
    data class Message(
        @Json(name = "inputs")
        val inputs: List<Item>,
        @Json(name = "outputs")
        val outputs: List<Item>,
    ) {

        @JsonClass(generateAdapter = true)
        data class Item(
            @Json(name = "address")
            val address: String,
            @Json(name = "coins")
            val coins: List<Coin>,
        ) {

            @JsonClass(generateAdapter = true)
            data class Coin(
                @Json(name = "amount")
                val amount: Long,
                @Json(name = "denom")
                val denom: String,
            )
        }
    }
}