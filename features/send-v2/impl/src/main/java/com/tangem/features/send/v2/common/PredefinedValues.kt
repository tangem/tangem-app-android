package com.tangem.features.send.v2.common

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
        ) : Content()
    }
}