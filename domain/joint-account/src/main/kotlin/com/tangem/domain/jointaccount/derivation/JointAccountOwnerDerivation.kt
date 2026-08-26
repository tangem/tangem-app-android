package com.tangem.domain.jointaccount.derivation

import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.network.Network
import java.util.Locale

/**
 * The joint account owner key derivation path. Must match other platforms — pinned by a unit test.
 * The index tells the owner keys of a participant's joint accounts apart.
 */
const val JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE: String = "m/44'/60'/888888'/0/%d"

private val ownerDerivationPathPrefix: String = JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE.substringBefore("%d")

/**
 * Whether [rawPath] lies in the owner key namespace of joint accounts. No key of the wallet's own accounts sits
 * there, so a path in it never stands for an address of the wallet — the funds are at the Safe address.
 */
fun isJointAccountOwnerDerivationPath(rawPath: String): Boolean = rawPath.startsWith(ownerDerivationPathPrefix)

fun jointAccountOwnerDerivationPath(index: Int): DerivationPath {
    require(index >= 0) { "Owner derivation index cannot be negative: $index" }

    // Locale.ROOT: %d on an Arabic-Indic digit locale would emit localized digits into rawPath
    return DerivationPath(rawPath = JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE.format(Locale.ROOT, index))
}

/** The network path of the currencies of the joint account whose owner key sits at this index. */
fun OwnerKeyIndex.toNetworkDerivationPath(): Network.DerivationPath.Joint {
    return Network.DerivationPath.Joint(value = jointAccountOwnerDerivationPath(index = value).rawPath)
}