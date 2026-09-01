package com.tangem.domain.jointaccount.signing

/**
 * A payload signed by the card in a joint account flow: EIP-191 (`0x45`) over the RFC 8785 canonical JSON
 * of [toCanonicalMap]. Strings go into the map exactly as the user entered them, never normalized or trimmed —
 * the backend canonicalizes the received payload as is and recovers the signer address from the signature.
 */
interface JointAccountSignablePayload {

    fun toCanonicalMap(): Map<String, Any>
}