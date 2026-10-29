package com.tangem.domain.jointaccount.model

/** An issued, unredeemed invite as the list reports it: without the code, see [JointAccountIssuedInvite] */
data class JointAccountInvite(
    val slot: Int,
    val expiresAtMillis: Long,
) {

    fun isExpired(nowMillis: Long): Boolean = expiresAtMillis <= nowMillis
}