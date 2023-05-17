package com.tangem.domain.card
// [REDACTED_TODO_COMMENT]
sealed class ScanCardException : Exception() {
    object WrongCardId : ScanCardException()

    object UserCancelled : ScanCardException()

    object WrongAccessCode : ScanCardException()

    open class ChainException : ScanCardException()

    class UnknownException(override val cause: Exception) : ScanCardException()
}
