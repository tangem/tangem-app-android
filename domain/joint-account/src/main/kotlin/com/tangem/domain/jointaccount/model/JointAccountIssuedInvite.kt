package com.tangem.domain.jointaccount.model

/** [id] is the invite code: the backend returns it here and nowhere else, and the app never stores it */
data class JointAccountIssuedInvite(
    val id: String,
    val slot: Int,
    val expiresAtMillis: Long,
)