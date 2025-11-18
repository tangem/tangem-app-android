package com.tangem.domain.card

sealed class ScanCardException : Exception() {

    class UserCancelled : ScanCardException() {
        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = UserCancelled()
    }

    class WrongAccessCode : ScanCardException() {
        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = WrongAccessCode()
    }

    open class ChainException : ScanCardException()

    data class UnknownException(override val cause: Throwable) : ScanCardException()

    data class WrongCardId(val cardId: String) : ScanCardException()
}