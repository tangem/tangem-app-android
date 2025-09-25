package com.tangem.lib.crypto.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import timber.log.Timber

/** Extension function to convert a [DerivationPath] into a [MutableDerivationPath] */
fun DerivationPath.toMutable(): MutableDerivationPath = MutableDerivationPath(value = this)

/**
 * A mutable representation of a derivation path, allowing modifications to specific nodes
 *
 * @property value the initial derivationPath to be used
 */
class MutableDerivationPath internal constructor(val value: DerivationPath) {

    /**
     * Replaces the account node in the derivation path with a new value
     *
     * @param value      the new index to set for the account node
     * @param blockchain the blockchain used to determine the account node index
     */
    fun replaceAccountNode(value: Long, blockchain: Blockchain): MutableDerivationPath {
        val mutableNodes = this@MutableDerivationPath.value.nodes.toMutableList()

        val accountNodeIndex = AccountNodeRecognizer(blockchain).accountNodeIndex
        val accountNode = mutableNodes.getOrNull(accountNodeIndex)

        if (accountNode != null) {
            mutableNodes[accountNodeIndex] = when (accountNode) {
                is DerivationNode.Hardened -> DerivationNode.Hardened(value)
                is DerivationNode.NonHardened -> DerivationNode.NonHardened(value)
            }
        } else {
            Timber.e("Account node not found in the derivation path: ${this@MutableDerivationPath.value}")
        }

        return DerivationPath(path = mutableNodes).toMutable()
    }

    /** Applies the changes and returns it as an immutable [DerivationPath] */
    fun apply(): DerivationPath = value
}