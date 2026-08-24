package com.tangem.features.jointaccount.join.model

/**
 * @property inviteId one-time invite secret the flow was opened with
 * @property draftHolder accumulator of the join flow steps
 */
internal data class JointAccountJoinChildParams(
    val inviteId: String,
    val draftHolder: JointAccountJoinDraftHolder,
)