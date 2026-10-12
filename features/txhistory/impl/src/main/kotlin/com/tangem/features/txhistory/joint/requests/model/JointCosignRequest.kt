package com.tangem.features.txhistory.joint.requests.model

import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer

internal data class JointCosignRequest(
    val operation: JointOperation,
    val transfer: JointOperationTransfer,
)