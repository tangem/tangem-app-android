package com.tangem.domain.visa.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Result of deriving the Virtual Account key on the card.
 *
 * @property address     the VA deposit address generated from the derived key
 * @property derivedKeys the derived extended public key(s) keyed by the seed wallet public key,
 *                       ready to be persisted into the wallet (see `DerivationsRepository.storeDerivedKeys`)
 */
data class VirtualAccountActivationData(
    val address: String,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)