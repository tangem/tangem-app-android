package com.tangem.data.collectibles.error

/** The collectibles backend's own error codes, sent in an error body or as a transaction's `packOpeningErrorCode`. */
internal object CollectiblesErrorCodes {

    const val PACK_NOT_READY = 3009
    const val PACK_OPENING_FAILED = 3010
    const val LOW_INVENTORY = 3016

    // TODO([REDACTED_TASK_KEY]): to be defined, a placeholder until the backend names the code
    const val REFUND_IN_PROGRESS = 3999
}