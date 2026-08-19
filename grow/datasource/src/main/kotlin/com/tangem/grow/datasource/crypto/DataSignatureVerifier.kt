package com.tangem.grow.datasource.crypto

interface DataSignatureVerifier {

    fun verifySignature(signature: String, data: String): Boolean
}