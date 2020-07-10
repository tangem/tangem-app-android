package com.tangem.common

import com.tangem.common.extensions.calculateSha256

class PinCode(
        val value: ByteArray,
        val isDefault: Boolean
) {
    constructor(value: String, isDefault: Boolean = false): this(value.calculateSha256(), isDefault)
}
