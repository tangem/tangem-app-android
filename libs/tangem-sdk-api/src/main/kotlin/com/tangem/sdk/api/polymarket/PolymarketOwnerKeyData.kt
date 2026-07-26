package com.tangem.sdk.api.polymarket

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Result of the Polymarket owner-key card session (APP-7a): the derived secp256k1 owner key(s), keyed by
 * the seed wallet public key, for the caller to persist via
 * [com.tangem.domain.wallets.derivations.DerivationsRepository.storeDerivedKeys] and to compute the
 * ERC-55 owner address from.
 */
data class PolymarketOwnerKeyData(
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)