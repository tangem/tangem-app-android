package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeedPhraseNotificationDTO(val status: Status) {

    enum class Status {
        @Json(name = "notneeded")
        NOT_NEEDED,

        @Json(name = "notified")
        NOTIFIED,

        @Json(name = "declined")
        DECLINED,

        @Json(name = "confirmed")
        CONFIRMED,

        @Json(name = "rejected")
        REJECTED,

        @Json(name = "accepted")
        ACCEPTED,
    }
}