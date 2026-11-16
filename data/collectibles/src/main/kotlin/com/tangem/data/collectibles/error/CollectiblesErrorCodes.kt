package com.tangem.data.collectibles.error

/** The collectibles backend's own error codes, sent in an error body or as a transaction's `packOpeningErrorCode`. */
internal object CollectiblesErrorCodes {

    const val LOW_INVENTORY = 4105

    const val PACK_OPENING_FAILED = 4200

    /** Collector Crypt refused the opening for good (unknown memo, 2 hours passed, no NFT): the purchase is refunded. */
    const val REFUND_IN_PROGRESS = 4201
    const val PACK_NOT_READY = 4202
    const val PACK_RECIPIENT_BLOCKED = 4203
    const val MACHINE_OUT_OF_NFT = 4204
}