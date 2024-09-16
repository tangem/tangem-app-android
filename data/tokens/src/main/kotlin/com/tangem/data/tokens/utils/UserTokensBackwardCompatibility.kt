package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

/**
 * Helper to apply compatibility changes for [UserTokensResponse] to support old saved tokens
 * in new application with new IDs
 */
class UserTokensBackwardCompatibility {

    fun applyCompatibilityAndGetUpdated(userTokensResponse: UserTokensResponse): UserTokensResponse {
        return userTokensResponse.copy(
            tokens = userTokensResponse.tokens.map { token ->
                val oldSavedId = NETWORKS_TO_OLD_SAVED_IDS[token.networkId]
                if (oldSavedId != null && token.id == oldSavedId) {
                    Blockchain.fromNetworkId(token.networkId)?.let { blockchain ->
                        token.copy(
                            id = blockchain.toCoinId(),
                        )
                    } ?: token
                } else {
                    token
                }
            },
        )
    }

    companion object {
        private val NETWORKS_TO_OLD_SAVED_IDS = mapOf(
            "optimistic-ethereum" to "ethereum",
            "arbitrum-one" to "ethereum",
        )
    }
}