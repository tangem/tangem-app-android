package com.tangem.domain.jointaccount.model

/** An unused invite of a joint account slot; [expiresAtMillis] is `null` until the backend reports a TTL */
data class JointAccountInvite(
    val id: String,
    val expiresAtMillis: Long? = null,
)