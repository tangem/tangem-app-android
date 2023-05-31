package com.tangem.domain.card
// [REDACTED_TODO_COMMENT]
sealed class ScanCardException : Exception() {
    object UserCancelled : ScanCardException()

    object WrongAccessCode : ScanCardException()

    open class ChainException : ScanCardException()

    data class UnknownException(override val cause: Exception) : ScanCardException()

    data class WrongCardId(val cardId: String) : ScanCardException()
}
