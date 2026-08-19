package com.tangem.grow.datasource.crypto

import com.tangem.common.extensions.hexToBytes
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.crypto.CryptoUtils
import com.tangem.grow.datasource.config.Express
import com.tangem.grow.datasource.config.GrowEnvironmentConfig

internal class Sha256SignatureVerifier(
    private val growEnvironmentConfig: GrowEnvironmentConfig,
    private val apiConfigsManager: ApiConfigsManager,
) : DataSignatureVerifier {

    override fun verifySignature(signature: String, data: String): Boolean {
        val pubKey = getPubKey() ?: return false
        return CryptoUtils.verify(
            publicKey = pubKey.hexToBytes().takeLast(n = 65).toByteArray(),
            message = data.toByteArray(),
            signature = signature.hexToBytes(),
        )
    }

    private fun getPubKey(): String? {
        val expressConfig = apiConfigsManager.getEnvironmentConfig(Express.ID)
        return when (expressConfig.environment) {
            ApiEnvironment.PROD -> growEnvironmentConfig.express?.signVerifierPublicKey
            else -> growEnvironmentConfig.devExpress?.signVerifierPublicKey
        }
    }
}