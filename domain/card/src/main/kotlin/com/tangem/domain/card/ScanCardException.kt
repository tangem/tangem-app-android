package com.tangem.domain.card

// TODO: May be add new error types
sealed class ScanCardException : Exception() {
    object WrongCardId : ScanCardException()

    object UserCancelled : ScanCardException()

    object WrongAccessCode : ScanCardException()

    open class ChainException : ScanCardException()

    class UnknownException(override val cause: Exception) : ScanCardException()
}