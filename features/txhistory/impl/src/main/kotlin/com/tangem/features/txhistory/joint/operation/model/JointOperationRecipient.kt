package com.tangem.features.txhistory.joint.operation.model

import com.tangem.features.txhistory.model.ResolvedOwner

internal data class JointOperationRecipient(
    val owner: ResolvedOwner,
    val canCreateContact: Boolean,
)