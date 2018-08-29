package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class CardVerifyModel(
        @SerializedName("results")
        var results: ArrayList<Verify>? = null,

        @SerializedName("error")
        var error: String? = null
) {

    data class Verify(
            @SerializedName("error")
            var error: String = "",

            @SerializedName("CID")
            var CID: String = "",

            @SerializedName("passed")
            var passed: Boolean? = null
    )

}