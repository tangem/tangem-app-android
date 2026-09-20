package com.tangem.domain.visa.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * @property address     the money account deposit address generated from the derived key
 * @property derivedKeys the derived extended public key(s) keyed by the seed wallet public key,
 *                       ready to be persisted into the wallet (see `DerivationsRepository.storeDerivedKeys`)
 */
data class MoneyAccountActivationData(
    val address: String,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)