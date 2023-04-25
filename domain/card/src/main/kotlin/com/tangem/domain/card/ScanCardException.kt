package com.tangem.domain.card

import com.tangem.common.core.TangemSdkError
// [REDACTED_TODO_COMMENT]
sealed class ScanCardException : Exception() {
    object WrongCardId : ScanCardException()

    object UserCancelled : ScanCardException()

    open class ChainException : ScanCardException()

    class SdkException(override val cause: TangemSdkError) : ScanCardException()
}
