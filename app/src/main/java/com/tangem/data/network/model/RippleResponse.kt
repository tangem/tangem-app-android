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
        var drops: FeeDrops? = null,

        //for RIPPLE_SUBMIT
        @SerializedName("engine_result")
        var engine_result: String? = null,

        //for RIPPLE_SUBMIT
        @SerializedName ("engine_result_message")
        var engine_result_message: String? = null
)

data class RippleAccountData(
        @SerializedName("Account")
        var account: String? = null,

        @SerializedName("Balance")
        var balance: String? = null,

        @SerializedName("Sequence")
        var sequence: Int? = null
)

data class FeeDrops(
        //enough to put tx to queue
        @SerializedName("minimum_fee")
        var minimum_fee: String? = null,

        //enough to put tx to current ledger
        @SerializedName("open_ledger_fee")
        var open_ledger_fee: String? = null,

        @SerializedName("median_fee")
        var median_fee: String? = null
)