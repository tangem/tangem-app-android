package com.tangem.features.send.v2.api.entity

sealed class PredefinedValues {
    data object Empty : PredefinedValues()

    sealed class Content : PredefinedValues() {
        abstract val address: String
        abstract val amount: String?
        abstract val memo: String?

        data class Deeplink(
            override val amount: String,
            override val address: String,
            override val memo: String?,
            val transactionId: String,
        ) : Content()

        data class QrCode(
            override val amount: String?,
            override val address: String,
            override val memo: String?,
            val source: Source,
        ) : Content()
    }

    enum class Source {
        MAIN_SCREEN,
        SEND_SCREEN,
    }
}

inline val PredefinedValues.isFromMainScreenQr: Boolean
    get() = (this as? PredefinedValues.Content.QrCode)
        ?.source == PredefinedValues.Source.MAIN_SCREEN