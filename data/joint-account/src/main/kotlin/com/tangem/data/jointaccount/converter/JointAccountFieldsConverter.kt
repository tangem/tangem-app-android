package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointAccount

/**
 * Raw backend strings → domain enums. An unknown value maps to `UNKNOWN` instead of failing: the read path has no
 * kill switch, so a value added by the backend later must never break parsing or the wallet screen.
 */
internal object JointAccountFieldsConverter {

    fun convertStatus(value: String): JointAccount.Status = when (value) {
        "pending" -> JointAccount.Status.PENDING
        "confirming" -> JointAccount.Status.CONFIRMING
        "active" -> JointAccount.Status.ACTIVE
        "cancelled" -> JointAccount.Status.CANCELLED
        else -> JointAccount.Status.UNKNOWN
    }

    fun convertRole(value: String): JointAccount.Role = when (value) {
        "creator" -> JointAccount.Role.CREATOR
        "member" -> JointAccount.Role.MEMBER
        else -> JointAccount.Role.UNKNOWN
    }
}