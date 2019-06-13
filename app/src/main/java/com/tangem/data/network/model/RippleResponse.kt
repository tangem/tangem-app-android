package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class RippleResponse(
        @SerializedName("result")
        var result: RippleResult? = null
)

data class RippleResult(
        @SerializedName("account_data")
        var account_data: RippleAccountData? = null,

        @SerializedName("validated")
        var validated: Boolean? = null,

        //for RIPPLE_FEE
        @SerializedName("drops")
        var drops: RippleFeeDrops? = null,

        //for RIPPLE_SUBMIT
        @SerializedName("engine_result_code")
        var engine_result_code: Int? = null,

        //for RIPPLE_SUBMIT
        @SerializedName ("engine_result_message")
        var engine_result_message: String? = null,

        //for RIPPLE_SUBMIT
        @SerializedName("error")
        var error: String? = null,

        //for RIPPLE_SUBMIT
        @SerializedName("error_exception")
        var error_exception: String? = null,

        //for RIPPLE_SERVER_STATE
        @SerializedName("state")
        var state: RippleState? = null,

        //for "Account not found error"
        @SerializedName("error_code")
        var error_code: Int? = null
)

data class RippleAccountData(
        @SerializedName("Account")
        var account: String? = null,

        @SerializedName("Balance")
        var balance: String? = null,

        @SerializedName("Sequence")
        var sequence: Long? = null
)

data class RippleFeeDrops(
        //enough to put tx to queue
        @SerializedName("minimum_fee")
        var minimum_fee: String? = null,

        //enough to put tx to current ledger
        @SerializedName("open_ledger_fee")
        var open_ledger_fee: String? = null,

        @SerializedName("median_fee")
        var median_fee: String? = null
)

data class RippleState(
        @SerializedName("validated_ledger")
        var validated_ledger: RippleLedger? = null
)

data class RippleLedger(
        @SerializedName("reserve_base")
        var reserve_base: Long? = null
)