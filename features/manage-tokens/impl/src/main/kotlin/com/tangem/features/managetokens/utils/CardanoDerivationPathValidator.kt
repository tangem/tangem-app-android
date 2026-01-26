package com.tangem.features.managetokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.network.Network

/**
 * Validator for Cardano derivation paths.
 * Cardano requires exactly 5 nodes in derivation path according to CIP-1852 standard.
 */
internal class CardanoDerivationPathValidator {

    /**
     * Checks if the derivation path is invalid for Cardano network.
     * @return true if network is Cardano AND path has insufficient nodes
     */
    fun isInvalidForCardano(networkId: Network.ID, path: String?): Boolean {
        if (!isCardano(networkId) || path == null) return false
        return !isValidDerivationPath(path)
    }

    private fun isCardano(networkId: Network.ID): Boolean {
        return networkId.toBlockchain() == Blockchain.Cardano
    }

    private fun isValidDerivationPath(path: String): Boolean {
        return runCatching {
            DerivationPath(rawPath = path).nodes.size == REQUIRED_DERIVATION_NODES
        }.getOrDefault(false)
    }

    private companion object {
        const val REQUIRED_DERIVATION_NODES = 5
    }
}