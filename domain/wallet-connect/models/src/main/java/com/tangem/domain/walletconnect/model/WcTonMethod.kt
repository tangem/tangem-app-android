package com.tangem.domain.walletconnect.model

sealed interface WcTonMethod : WcMethod {

    data class SendMessage(
        val validUntil: Long?,
        val from: String?,
        val messages: List<Message>,
    ) : WcTonMethod {
        data class Message(
            val address: String,
            val amount: String,
            val payload: String?,
            val stateInit: String?,
        )
    }

    data class SignData(
        val type: Type,
        val from: String?,
    ) : WcTonMethod {
        sealed interface Type {
            data class Text(val text: String) : Type
            data class Binary(val bytes: String) : Type
            data class Cell(val schema: String, val cell: String) : Type
        }
    }
}