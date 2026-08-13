package com.tangem.domain.jointaccount.model

/**
 * Input of the one-tap creation signing: everything known before the card is tapped.
 *

 * @property creatorName            the creator's display name, goes into the payload as entered
 * @property config                 the shared account config
 * @property firstCandidateIndex    the first owner derivation index to try, `wallet.totalJointAccounts` when known
 * @property occupiedOwnerAddresses owner addresses already registered in the wallet's joint accounts, from
 * `GET /joint-accounts` members; the task skips a candidate index whose address is among them (compared lowercased)
 * @property maxIndexAttempts       how many candidate indices to try before failing
 */
data class JointAccountCreationSignInput(
    val walletId: String,
    val creatorName: String,
    val config: JointAccountCreationPayload.Config,
    val firstCandidateIndex: Int,
    val occupiedOwnerAddresses: Set<String>,
    val maxIndexAttempts: Int = DEFAULT_MAX_INDEX_ATTEMPTS,
) {

    companion object {
        const val DEFAULT_MAX_INDEX_ATTEMPTS = 20
    }
}