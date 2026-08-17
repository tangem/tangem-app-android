package com.tangem.domain.jointaccount.derivation

import com.tangem.crypto.hdWallet.DerivationPath
import java.util.Locale

/**
 * The joint account owner key derivation path. Must match other platforms — pinned by a unit test.
 * The index tells the owner keys of a participant's joint accounts apart.
 */
const val JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE: String = "m/44'/60'/888888'/0/%d"

fun jointAccountOwnerDerivationPath(index: Int): DerivationPath {
    require(index >= 0) { "Owner derivation index cannot be negative: $index" }

    // Locale.ROOT: %d on an Arabic-Indic digit locale would emit localized digits into rawPath
    return DerivationPath(rawPath = JOINT_ACCOUNT_OWNER_DERIVATION_PATH_TEMPLATE.format(Locale.ROOT, index))
}