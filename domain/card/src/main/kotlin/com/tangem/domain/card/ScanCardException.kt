package com.tangem.domain.card

sealed class ScanCardException : Exception() {

    data object UserCancelled : ScanCardException() {
        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = UserCancelled
    }

    data object WrongAccessCode : ScanCardException() {
        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = WrongAccessCode
    }

    open class ChainException : ScanCardException()

    data class UnknownException(override val cause: Throwable) : ScanCardException()

    data class WrongCardId(val cardId: String) : ScanCardException()
}