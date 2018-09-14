package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

class CardVerifyAndGetArtwork
 {

     data class Request(
             @SerializedName("requests")
             var requests: List<RequestItem>? = null
     ) {

         data class RequestItem(
                 @SerializedName("CID")
                 var CID: String = "",

                 @SerializedName("publicKey")
                 var publicKey: String = "",

                 @SerializedName("artwork")
                 var artwork: String = ""
         )

     }

    data class Response(
            @SerializedName("results")
            var results: List<ResultItem>? = null
    ) {

        data class ResultItem(
                @SerializedName("error")
                var error: String = "",

                @SerializedName("CID")
                var CID: String = "",

                @SerializedName("passed")
                var passed: Boolean = false,

                @SerializedName("artwork")
                var artwork: String = "",

                @SerializedName("batch")
                var batch: String = "",

                @SerializedName("update_date")
                var update_date: String = ""
        )
    }

}