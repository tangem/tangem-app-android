package com.tangem.datasource.crypto

interface DataSignatureVerifier {

    fun verifySignature(signature: String, data: String): Boolean
}