package com.tangem.tangemserver.android.model

import java.text.SimpleDateFormat
import java.util.*

class CardVerifyAndGetInfo {
    data class Request(
            var requests: List<Item>? = null
    ) {

        data class Item(
                var CID: String = "",
                var publicKey: String = ""
        )

    }

    data class Response(
            var results: List<Item>? = null
    ) {

        data class Item(
                var error: String? = null,
                var CID: String = "",
                var passed: Boolean = false,
                var batch: String = "",
                var artwork: ArtworkInfo? = null,
                var substitution: SubstitutionInfo? = null
        ) {

            data class ArtworkInfo(
                    var id: String = "",
                    var hash: String = "",
                    var date: String = ""
            ) {
                fun getUpdateDate(): Date? {
                    return try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).parse(date)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

            }

            data class SubstitutionInfo(
                    var data: String? = null,
                    var signature: String? = null
            )

        }
    }

}