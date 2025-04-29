package com.tangem.features.onramp.deeplink

enum class OnrampRedirectResult(val value: String) {
    Success("success"),
    Cancel("cancel"),
    Unknown(""),
    ;

    companion object {
        fun getResult(result: String?): OnrampRedirectResult {
            return entries.firstOrNull { it.value == result } ?: Unknown
        }
    }
}