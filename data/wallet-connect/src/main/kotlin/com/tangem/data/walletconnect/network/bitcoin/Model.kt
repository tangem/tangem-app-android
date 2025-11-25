package com.tangem.data.walletconnect.network.bitcoin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * JSON request model for sendTransfer method.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinSendTransferRequest(
    @Json(name = "account")
    val account: String,

    @Json(name = "recipientAddress")
    val recipientAddress: String,

    @Json(name = "amount")
    val amount: String,

    @Json(name = "memo")
    val memo: String? = null,

    @Json(name = "changeAddress")
    val changeAddress: String? = null,
)

/**
 * JSON request model for getAccountAddresses method.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinGetAccountAddressesRequest(
    @Json(name = "account")
    val account: String? = null,

    @Json(name = "intentions")
    val intentions: List<String>? = null,
)

/**
 * JSON request model for signPsbt method.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinSignPsbtRequest(
    @Json(name = "psbt")
    val psbt: String,

    @Json(name = "signInputs")
    val signInputs: List<WcBitcoinSignInput>,

    @Json(name = "broadcast")
    val broadcast: Boolean? = false,
)

/**
 * JSON model for sign input specification.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinSignInput(
    @Json(name = "address")
    val address: String,

    @Json(name = "index")
    val index: Int,

    @Json(name = "sighashTypes")
    val sighashTypes: List<Int>? = null,
)

/**
 * JSON request model for signMessage method.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinSignMessageRequest(
    @Json(name = "account")
    val account: String,

    @Json(name = "message")
    val message: String,

    @Json(name = "address")
    val address: String? = null,

    @Json(name = "protocol")
    val protocol: String? = "ecdsa",
)

/**
 * JSON response model for getAccountAddresses method.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinGetAccountAddressesResponse(
    @Json(name = "addresses")
    val addresses: List<WcBitcoinAddressInfo>,
)

/**
 * Address information in getAccountAddresses response.
 */
@JsonClass(generateAdapter = true)
internal data class WcBitcoinAddressInfo(
    @Json(name = "address")
    val address: String,

    @Json(name = "publicKey")
    val publicKey: String? = null,

    @Json(name = "path")
    val path: String? = null,

    @Json(name = "intention")
    val intention: String? = null,
)