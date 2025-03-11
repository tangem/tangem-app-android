package com.tangem.lib.visa.utils

internal fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }