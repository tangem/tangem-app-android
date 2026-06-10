package com.tangem.domain.dynamicaddresses

import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Shared utility for checking if a derivation path conflicts with Dynamic Addresses.
 *
 * A path conflicts when it belongs to the same BIP44 account as the base path
 * (first 3 nodes: purpose / coin_type / account match) but has non-zero
 * change (node 3) or address_index (node 4).
 */
object DynamicAddressesDerivationChecker {

    const val BIP44_NODE_COUNT = 5
    private const val ACCOUNT_NODE_COUNT = 3
    private const val CHANGE_NODE_INDEX = 3
    private const val ADDRESS_INDEX_NODE_INDEX = 4

    fun parseNodes(path: String): List<DerivationNode>? {
        return runCatching { DerivationPath(path).nodes }.getOrNull()
    }

    /**
     * @return `true` if [path] has zero change (node 3) and zero address_index (node 4).
     */
    fun isBaseDerivation(path: String): Boolean {
        val nodes = parseNodes(path) ?: return false
        return isBaseDerivation(nodes)
    }

    fun isBaseDerivation(nodes: List<DerivationNode>): Boolean {
        if (nodes.size < BIP44_NODE_COUNT) return false

        val change = nodes[CHANGE_NODE_INDEX].getIndex(includeHardened = false)
        val index = nodes[ADDRESS_INDEX_NODE_INDEX].getIndex(includeHardened = false)

        return change == 0L && index == 0L
    }

    /**
     * @return `true` if [customPath] shares the same account as [basePath] but has
     *         non-zero change or address_index nodes.
     */
    fun hasSameAccountWithNonZeroChangeOrIndex(customPath: String, basePath: String): Boolean {
        val customNodes = runCatching { DerivationPath(customPath).nodes }.getOrNull() ?: return false
        val baseNodes = runCatching { DerivationPath(basePath).nodes }.getOrNull() ?: return false

        if (customNodes.size < BIP44_NODE_COUNT || baseNodes.size < BIP44_NODE_COUNT) return false

        val isSameAccount = (0 until ACCOUNT_NODE_COUNT).all { i ->
            customNodes[i].getIndex(includeHardened = false) == baseNodes[i].getIndex(includeHardened = false)
        }
        if (!isSameAccount) return false

        val change = customNodes[CHANGE_NODE_INDEX].getIndex(includeHardened = false)
        val index = customNodes[ADDRESS_INDEX_NODE_INDEX].getIndex(includeHardened = false)

        return change != 0L || index != 0L
    }
}