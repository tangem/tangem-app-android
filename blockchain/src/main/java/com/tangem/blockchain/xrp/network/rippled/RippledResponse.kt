package com.tangem.blockchain.xrp.network.rippled

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Rippled account
@JsonClass(generateAdapter = true)
data class RippledAccountResponse(
        @Json(name = "result")
        var result: RippledAccountResult? = null
)

@JsonClass(generateAdapter = true)
data class RippledAccountResult(
        @Json(name = "account_data")
        var accountData: RippledAccountData? = null,

        @Json(name = "error_code")
        var errorCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class RippledAccountData(
        @Json(name = "Balance")
        var balance: String? = null,

        @Json(name = "Sequence")
        var sequence: Long? = null
)

// Rippled state
@JsonClass(generateAdapter = true)
data class RippledStateResponse(
        @Json(name = "result")
        var result: RippledStateResult? = null
)

@JsonClass(generateAdapter = true)
data class RippledStateResult(
        @Json(name = "state")
        var state: RippledState? = null
)

@JsonClass(generateAdapter = true)
data class RippledState(
        @Json(name = "validated_ledger")
        var validatedLedger: RippledLedger? = null
)

@JsonClass(generateAdapter = true)
data class RippledLedger(
        @Json(name = "reserve_base")
        var reserveBase: Long? = null
)

// Rippled fee
@JsonClass(generateAdapter = true)
data class RippledFeeResponse(
        @Json(name = "result")
        var result: RippledFeeResult? = null
)

@JsonClass(generateAdapter = true)
data class RippledFeeResult(
        @Json(name = "drops")
        var feeData: RippledFeeData? = null
)

@JsonClass(generateAdapter = true)
data class RippledFeeData(
        //enough to put tx to queue
        @Json(name = "minimum_fee")
        var minimalFee: String? = null,

        //enough to put tx to current ledger
        @Json(name = "open_ledger_fee")
        var normalFee: String? = null,

        @Json(name = "median_fee")
        var priorityFee: String? = null
)

// Rippled submit
@JsonClass(generateAdapter = true)
data class RippledSubmitResponse(
        @Json(name = "result")
        var result: RippledSubmitResult? = null
)

@JsonClass(generateAdapter = true)
data class RippledSubmitResult(
        @Json(name = "engine_result_code")
        var resultCode: Int? = null,

        @Json(name = "engine_result_message")
        var resultMessage: String? = null,

        @Json(name = "error")
        var error: String? = null,

        @Json(name = "error_exception")
        var errorException: String? = null
)