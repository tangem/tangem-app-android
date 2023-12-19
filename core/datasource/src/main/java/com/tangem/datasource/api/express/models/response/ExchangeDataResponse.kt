package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ExchangeDataResponse(
    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "fromDecimals")
    val fromDecimals: Int,

    @Json(name = "toAmount")
    val toAmount: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "txType")
    val txType: TxType,

    @Json(name = "txId")
    val txId: String, // inner tangem-express transaction id

    @Json(name = "txFrom")
    val txFrom: String?, // account for debiting tokens (same as toAddress) if DEX, null if CEX

    @Json(name = "txTo")
    val txTo: String, // swap smart-contract address if DEX, address for sending transaction if CEX

    @Json(name = "txData")
    val txData: String?, // transaction data if DEX, null if CEX

    @Json(name = "txValue")
    val txValue: BigDecimal, // amount (same as fromAmount)

    @Json(name = "externalTxId")
    val externalTxId: String?, // null if DEX, provider transaction id if CEX

    @Json(name = "externalTxUrl")
    val externalTxUrl: String?, // null if DEX, url of provider exchange status page if CEX

    @Json(name = "txExtraIdName")
    val txExtraIdName: String?,

    @Json(name = "txExtraId")
    val txExtraId: String?,
)

enum class TxType {
    @Json(name = "send")
    SEND,

    @Json(name = "swap")
    SWAP,
}