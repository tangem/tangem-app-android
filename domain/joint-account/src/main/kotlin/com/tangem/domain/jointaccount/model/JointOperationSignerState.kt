package com.tangem.domain.jointaccount.model

/**
 * One member's standing on a pending operation, for the signers list.
 *
 * @property member the roster entry from [JointAccount.members]

 * member the backend has no record from yet
 * @property at when the member answered, ISO-8601 as the backend sent it; `null` while waiting
 */
data class JointOperationSignerState(
    val member: JointAccount.Member,
    val answer: Answer,
    val at: String?,
) {
    enum class Answer {
        CREATED,
        SIGNED,
        DECLINED,
        WAITING,
    }
}