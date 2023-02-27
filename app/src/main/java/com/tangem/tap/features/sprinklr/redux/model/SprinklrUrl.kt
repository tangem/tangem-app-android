package com.tangem.tap.features.sprinklr.redux.model

internal sealed class SprinklrUrl {
    abstract val url: String

    class Prod(userId: String, baseUrl: String, appId: String) : SprinklrUrl() {
        private val locale = java.util.Locale.getDefault().language

        override val url: String = "$baseUrl/page" +
            "?appId=$appId" +
            "&device=$Device" +
            "&enableClose=$CloseEnabled" +
            "&zoom=$ZoomEnabled" +
            "&locale=$locale" +
            "&user_id=$userId"

        companion object {
            private const val Device = "MOBILE"
            private const val CloseEnabled = true
            private const val ZoomEnabled = false
        }
    }

    object Static : SprinklrUrl() {
        override val url: String = "live-chat-static.sprinklr.com"
    }
}
