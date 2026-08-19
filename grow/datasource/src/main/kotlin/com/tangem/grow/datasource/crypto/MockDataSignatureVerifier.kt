package com.tangem.grow.datasource.crypto

internal class MockDataSignatureVerifier : DataSignatureVerifier {

    override fun verifySignature(signature: String, data: String): Boolean = true
}