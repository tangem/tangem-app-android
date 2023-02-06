package com.tangem.tap.domain.userWalletList.utils

internal fun List<ByteArray>.containsBA(element: ByteArray?): Boolean {
    this.forEach {
        if (it.contentEquals(element)) return true
    }

    return false
}
