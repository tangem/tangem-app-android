package com.tangem.datasource.crypto

import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.datasource.config.ConfigManager

internal class Sha256SignatureVerifier(private val configManager: ConfigManager) : DataSignatureVerifier {

    override fun verifySignature(signature: String, data: String): Boolean {
        val pubKey = configManager.config.express?.signVerifierPublicKey ?: return false
        return CryptoUtils.verify(
            publicKey = pubKey.hexToBytes().takeLast(n = 65).toByteArray(),
            message = data.toByteArray(),
            signature = signature.hexToBytes(),
        )
    }
}