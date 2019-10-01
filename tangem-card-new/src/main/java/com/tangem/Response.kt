package com.tangem

data class SignResponse(
        val cid: String,
        val signature: ByteArray,
        val remainingSignatures: Int,
        val signedHashes: Int
)


data class CardError(
        val code: Int = 0
)




