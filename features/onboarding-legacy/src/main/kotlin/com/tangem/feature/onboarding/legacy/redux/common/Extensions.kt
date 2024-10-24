package com.tangem.feature.onboarding.legacy.redux.common

internal fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
