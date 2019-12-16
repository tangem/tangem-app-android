package com.tangem.blockchain.extensions

import org.spongycastle.crypto.digests.RIPEMD160Digest

fun ByteArray.calculateRipemd160(): ByteArray {
    val digest = RIPEMD160Digest()
    digest.update(this, 0, this.size)
    val out = ByteArray(20)
    digest.doFinal(out, 0)
    return out
}