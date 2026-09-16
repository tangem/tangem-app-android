package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload

/**
 * `payload` of `POST .../operations/{operationId}/reject`. The [safeTx] numbers are decimal strings, as on the wire,
 * so the backend canonicalizes the received body into the bytes that were signed.
 */
data class JointOperationRejectPayload(
    val operationId: String,
    val safeTx: SafeTx,
) : JointAccountSignablePayload {

    override fun toCanonicalMap(): Map<String, Any> = mapOf(
        "operationId" to operationId,
        "safeTx" to mapOf(
            "to" to safeTx.to,
            "value" to safeTx.value.toString(),
            "data" to safeTx.data,
            "operation" to safeTx.operation,
            "safeTxGas" to safeTx.safeTxGas.toString(),
            "baseGas" to safeTx.baseGas.toString(),
            "gasPrice" to safeTx.gasPrice.toString(),
            "gasToken" to safeTx.gasToken,
            "refundReceiver" to safeTx.refundReceiver,
            "nonce" to safeTx.nonce.toString(),
        ),
    )
}