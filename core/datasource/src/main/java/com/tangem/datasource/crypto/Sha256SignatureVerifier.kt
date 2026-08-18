package com.tangem.datasource.crypto

import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.datasource.local.config.environment.EnvironmentConfig

internal class Sha256SignatureVerifier(
    private val environmentConfig: EnvironmentConfig,
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
        // The Express config now lives in grow:datasource, which core:datasource must not depend on;
        // look it up by its stable id (mirrors grow's Express.KEY).
        val expressConfig = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID(EXPRESS_CONFIG_ID))
        return when (expressConfig.environment) {
            ApiEnvironment.PROD -> environmentConfig.express?.signVerifierPublicKey
            else -> environmentConfig.devExpress?.signVerifierPublicKey
        }
    }

    private companion object {
        const val EXPRESS_CONFIG_ID = "Express"
    }
}