package com.tangem.domain.jointaccount.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Result of the one-tap creation signing. The owner key is derived at
 * [JointAccountCreationSignInput.derivationIndex].
 *
 * @property ownerAddress     the owner address, EIP-55 checksummed — `creator.address` of the signed payload
 * @property canonicalPayload the exact bytes that were signed (RFC 8785 canonical JSON of the payload). The request
 * must carry these bytes, not a re-serialization: re-encoding is not guaranteed to reproduce them
 * @property signature        EIP-191 signature over [canonicalPayload], `0x` + 130 hex chars (R‖S‖V, v ∈ {27, 28})
 * @property derivedKeys      the derived key to persist via `DerivationsRepository.storeDerivedKeys` — the card
 * session's in-memory copy dies with the session
 */
class JointAccountCreationSignResult(
    val ownerAddress: String,
    val canonicalPayload: ByteArray,
    val signature: String,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)