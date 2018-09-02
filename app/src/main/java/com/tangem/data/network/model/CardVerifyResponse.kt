package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class CardVerifyResponse(
        @SerializedName("results")
        var results: List<Verify>? = null
) {

    data class Verify(
            @SerializedName("CID")
            var CID: String = "",

            @SerializedName("passed")
            var passed: Boolean = false
    )

}