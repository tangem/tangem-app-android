package com.tangem.common.utils

/**
 * Helper for inspecting individual nodes of a BIP-44-style derivation path string
 * (e.g. one read from `Network.derivationPath` of a token in the domain account model).
 */
object DerivationPathHelper {

    /**
     * Returns the [index1Based]-th node of a derivation path, ignoring the leading `m`.
     * For "m/44'/0'/1'/0/0": node 1 = "44'", node 3 = "1'", node 5 = "0".
     */
    fun nodeAt(derivationPath: String, index1Based: Int): String {
        val nodes = derivationPath.removePrefix("m/").split("/")
        require(index1Based in 1..nodes.size) {
            "Node #$index1Based is out of range for path '$derivationPath' (${nodes.size} nodes)"
        }
        return nodes[index1Based - 1]
    }
}