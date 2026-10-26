package com.tangem.domain.jointaccount.model

/**
 * An issued, unredeemed invite of a joint account slot as the list reports it: without the code, which the backend
 * returns once at issue time. An expired invite stays listed until the slot is reissued.
 */
data class JointAccountInvite(
    val slot: Int,
    val expiresAtMillis: Long,
) {

    fun isExpired(nowMillis: Long): Boolean = expiresAtMillis <= nowMillis
}