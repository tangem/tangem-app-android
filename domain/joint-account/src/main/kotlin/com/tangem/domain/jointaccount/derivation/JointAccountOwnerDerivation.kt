package com.tangem.domain.jointaccount.derivation

import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
import java.util.Locale

/**
 * The joint account owner key derivation path. Must match other platforms — pinned by a unit test.
 * The index tells the owner keys of a participant's joint accounts apart.
 */
const val JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE: String = "m/44'/60'/888888'/0/%d"

/** The hardened node every joint owner path lives under — read off the template so the two cannot diverge. */
private val JOINT_ACCOUNT_RESERVED_NODE: DerivationNode by lazy {
    jointAccountOwnerDerivationPath(index = 0).nodes[RESERVED_NODE_POSITION]
}

/**
 * Whether [path] enters the reserved joint-account node. The whole node is reserved rather than the exact
 * owner paths: a custom currency under it would collide with a joint currency in every store keyed by path.
 */
fun isJointAccountReservedPath(path: DerivationPath): Boolean {
    return path.nodes.any { node -> node == JOINT_ACCOUNT_RESERVED_NODE }
}

/** [isJointAccountReservedPath] for a raw string; a path that does not parse reserves nothing. */
fun isJointAccountReservedPath(rawPath: String): Boolean {
    val path = runCatching { DerivationPath(rawPath) }.getOrNull() ?: return false
    return isJointAccountReservedPath(path)
}

private const val RESERVED_NODE_POSITION = 2

fun jointAccountOwnerDerivationPath(index: Int): DerivationPath {
    require(index >= 0) { "Owner derivation index cannot be negative: $index" }

    // Locale.ROOT: %d on an Arabic-Indic digit locale would emit localized digits into rawPath
    return DerivationPath(rawPath = JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE.format(Locale.ROOT, index))
}

/** The network path of the currencies of the joint account whose owner key sits at this index. */
fun OwnerKeyIndex.toNetworkDerivationPath(): Network.DerivationPath.Joint {
    return Network.DerivationPath.Joint(value = jointAccountOwnerDerivationPath(index = value).rawPath)
}