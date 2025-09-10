package com.tangem.lib.crypto.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.isUTXO
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Utility class to recognize the account node in a derivation path based on the blockchain type.
 * Derivation path schema: [ m / purpose' / coin_type' / account' / change / address_index ].
 *
 * @param blockchain the blockchain for which the account node is to be recognized
 *
[REDACTED_AUTHOR]
 */
class AccountNodeRecognizer(blockchain: Blockchain) {

    /** Index of the account node in the derivation path */
    val accountNodeIndex: Int = if (blockchain.isUTXO) {
        UTXO_BLOCKCHAIN_NODE_INDEX
    } else {
        NON_UTXO_BLOCKCHAIN_NODE_INDEX
    }

    /** Recognizes the account node value from the given derivation path string [derivationPathValue] */
    fun recognize(derivationPathValue: String): Long? {
        return runCatching {
            recognize(derivationPath = DerivationPath(rawPath = derivationPathValue))
        }
            .getOrNull()
    }

    /** Recognizes the account node value from the given [derivationPath] */
    fun recognize(derivationPath: DerivationPath): Long? {
        return runCatching {
            val accountNode = derivationPath.nodes.getOrNull(accountNodeIndex)

            accountNode?.getIndex(includeHardened = false)
        }
            .getOrNull()
    }

    private companion object {
        const val UTXO_BLOCKCHAIN_NODE_INDEX = 2
        const val NON_UTXO_BLOCKCHAIN_NODE_INDEX = 4
    }
}