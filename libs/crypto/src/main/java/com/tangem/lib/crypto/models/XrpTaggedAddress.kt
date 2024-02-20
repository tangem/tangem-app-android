package com.tangem.lib.crypto.models

data class XrpTaggedAddress(
    val address: String,
    val destinationTag: Long?,
)