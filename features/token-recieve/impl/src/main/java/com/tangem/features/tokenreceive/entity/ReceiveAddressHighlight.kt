package com.tangem.features.tokenreceive.entity

private const val ADDRESS_EDGE_LENGTH = 5
private const val ADDRESS_MIN_SPLIT_LENGTH = 15

/** Characters of [address] shown dimmed, or `null` when the whole address is shown in full color. */
internal fun receiveAddressDimmedRange(address: String): IntRange? = if (address.length < ADDRESS_MIN_SPLIT_LENGTH) {
    null
} else {
    ADDRESS_EDGE_LENGTH until address.length - ADDRESS_EDGE_LENGTH
}