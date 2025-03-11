package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeDataResponseWithTxDetails(
    @Json(name = "dataResponse")
    val dataResponse: ExchangeDataResponse,
    @Json(name = "txDetails")
    val txDetails: TxDetails,
)

@JsonClass(generateAdapter = true)
data class ExchangeDataResponse(
    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "fromDecimals")
    val fromDecimals: Int,

    @Json(name = "toAmount")
    val toAmount: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "txId")
    val txId: String, // inner tangem-express transaction id

    @Json(name = "txDetailsJson")
    val txDetailsJson: String,

    @Json(name = "signature")
    val signature: String,
)

@JsonClass(generateAdapter = true)
data class TxDetails(
    @Json(name = "payoutAddress")
    val payoutAddress: String,

    @Json(name = "requestId")
    val requestId: String,

    @Json(name = "txType")
    val txType: TxType,

    @Json(name = "txFrom")
    val txFrom: String?, // account for debiting tokens (same as toAddress) if DEX, null if CEX

    @Json(name = "txTo")
    val txTo: String, // swap smart-contract address if DEX, address for sending transaction if CEX

    @Json(name = "txData")
    val txData: String?, // transaction data if DEX, null if CEX

    @Json(name = "txValue")
    val txValue: String, // amount (same as fromAmount for Coin, but for bridge equal to otherNativeFee)

    @Json(name = "otherNativeFee")
    val otherNativeFee: String?,

    @Json(name = "externalTxId")
    val externalTxId: String?, // null if DEX, provider transaction id if CEX

    @Json(name = "externalTxUrl")
    val externalTxUrl: String?, // null if DEX, url of provider exchange status page if CEX

    @Json(name = "txExtraIdName")
    val txExtraIdName: String?,

    @Json(name = "txExtraId")
    val txExtraId: String?,

    @Json(name = "gas")
    val gas: String?,
)

@JsonClass(generateAdapter = false)
enum class TxType {
    @Json(name = "send")
    SEND,

    @Json(name = "swap")
    SWAP,
}