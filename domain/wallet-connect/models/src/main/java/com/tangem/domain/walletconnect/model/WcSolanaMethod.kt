package com.tangem.domain.walletconnect.model

sealed interface WcSolanaMethod : WcMethod {

    val methodName: String
    val trimmedPrefixMethodName: String get() = methodName.substringAfter("_")

    data class SignMessage(
        val pubKey: String,
        val rawMessage: String,
        val humanMsg: String,
    ) : WcSolanaMethod {
        override val methodName: String = WcSolanaMethodName.SignMessage.raw
    }

    data class SignTransaction(
        val transaction: String,
        val address: String?,
    ) : WcSolanaMethod {
        override val methodName: String = WcSolanaMethodName.SignTransaction.raw
    }

    data class SignAllTransaction(
        val transaction: List<String>,
    ) : WcSolanaMethod {
        override val methodName: String = WcSolanaMethodName.SendAllTransaction.raw
    }
}