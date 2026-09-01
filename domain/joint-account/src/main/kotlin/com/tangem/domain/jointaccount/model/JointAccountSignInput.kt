package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload
import com.tangem.domain.models.account.DerivationIndex

/**
 * Input of the one-tap signing: the owner derivation index and the payload built around the derived address.
 *
 * @property derivationIndex the owner derivation index, already validated — [DerivationIndex] rejects a negative
 * value with a typed error instead of throwing. For creation and joining it comes from
 * `wallet.totalJointAccounts`, read with a fresh accounts request right before the tap and never from cache: a
 * stale counter produces an occupied index, which surfaces only as a 409 after the tap is already spent. For

 * @property makePayload     builds the payload once the owner address is derived in the session. A payload that
 * carries a participant must put [derivationIndex] into its `derivation` — the backend recovers the signer from
 * the signature and matches it against the participant's address
 */
class JointAccountSignInput(
    val derivationIndex: DerivationIndex,
    val makePayload: (ownerAddress: String) -> JointAccountSignablePayload,
)