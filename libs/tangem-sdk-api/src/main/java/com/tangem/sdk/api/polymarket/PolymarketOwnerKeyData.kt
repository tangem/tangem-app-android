package com.tangem.sdk.api.polymarket

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Result of the Polymarket owner-key card session (APP-7a): the ERC-55 owner address plus the derived
 * secp256k1 key, keyed by the seed wallet public key for persistence via
 * [com.tangem.domain.wallets.derivations.DerivationsRepository.storeDerivedKeys].
 */
data class PolymarketOwnerKeyData(
    val address: String,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)