package com.tangem.domain.jointaccount.model

/**
 * Input of the one-tap creation signing: everything known before the card is tapped.
 *

 * @property creatorName     the creator's display name, goes into the payload as entered
 * @property config          the shared account config
 * @property derivationIndex the owner derivation index — `wallet.totalJointAccounts` read with a fresh accounts
 * request right before the tap, never from cache: a stale counter produces an occupied index, which surfaces only
 * as a 409 on `POST /joint-accounts` after the tap is already spent
 */
data class JointAccountCreationSignInput(
    val walletId: String,
    val creatorName: String,
    val config: JointAccountCreationPayload.Config,
    val derivationIndex: Int,
) {

    init {
        require(derivationIndex >= 0) { "Derivation index cannot be negative: $derivationIndex" }
    }
}