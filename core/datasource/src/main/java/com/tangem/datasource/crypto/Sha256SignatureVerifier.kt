package com.tangem.datasource.crypto

import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.config.ConfigManager

internal class Sha256SignatureVerifier(
    private val configManager: ConfigManager,
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
        val expressConfig = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID.Express)
        return when (expressConfig.environment) {
            ApiEnvironment.PROD -> configManager.getConfigSync().express?.signVerifierPublicKey
            else -> configManager.getConfigSync().devExpress?.signVerifierPublicKey
        }
    }
}
