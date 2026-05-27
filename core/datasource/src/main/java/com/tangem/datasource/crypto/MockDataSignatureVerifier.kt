package com.tangem.datasource.crypto

internal class MockDataSignatureVerifier : DataSignatureVerifier {

    override fun verifySignature(signature: String, data: String): Boolean = true
}