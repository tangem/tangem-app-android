package com.tangem.data.network.model

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
                fun getUpdateDate(): Instant? {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC)
                        } else {
                            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).parse(date).toInstant()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return null
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