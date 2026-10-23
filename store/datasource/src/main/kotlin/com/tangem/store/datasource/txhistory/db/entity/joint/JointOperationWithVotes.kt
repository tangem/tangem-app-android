package com.tangem.store.datasource.txhistory.db.entity.joint

data class JointOperationWithVotes(
    val operation: JointOperationEntity,
    val votes: List<JointOperationVoteEntity>,
)