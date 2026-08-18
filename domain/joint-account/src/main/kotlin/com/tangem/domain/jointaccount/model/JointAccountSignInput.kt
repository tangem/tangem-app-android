package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * Input of the one-tap signing: the owner derivation index and the payload built around the derived address.
 *
 * @property derivationIndex the owner derivation index. For creation and joining it is `wallet.totalJointAccounts`
 * read with a fresh accounts request right before the tap, never from cache: a stale counter produces an occupied
 * index, which surfaces only as a 409 after the tap is already spent. For activation it is the creator's own

 * @property makePayload     builds the payload once the owner address is derived in the session. A payload that
 * carries a participant must put [derivationIndex] into its `derivation` — the backend recovers the signer from
 * the signature and matches it against the participant's address
 */
class JointAccountSignInput(
    val derivationIndex: Int,
    val makePayload: (ownerAddress: String) -> JointAccountSignablePayload,
) {

    init {
        require(derivationIndex >= 0) { "Derivation index cannot be negative: $derivationIndex" }
    }
}