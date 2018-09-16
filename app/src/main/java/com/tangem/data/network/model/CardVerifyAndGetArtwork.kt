package com.tangem.data.network.model

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class CardVerifyAndGetArtwork {
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
                var artworkId: String = "",
                var artworkHash: String = "",
                var updateDate: String = ""
        ) {
            fun getUpdateDate(): Instant? {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return try {
                        LocalDateTime.parse(updateDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else {
                    return try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).parse(updateDate).toInstant()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
    }

}