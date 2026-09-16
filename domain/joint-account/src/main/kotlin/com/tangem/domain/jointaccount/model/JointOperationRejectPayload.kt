package com.tangem.domain.jointaccount.model

/** `payload` of `POST .../operations/{operationId}/reject`: the operation as the user saw it, for the backend to compare */
data class JointOperationRejectPayload(
    val operationId: String,
    val safeTx: SafeTx,
)