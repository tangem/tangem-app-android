package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A Safe operation as the backend returns it: inside [JointAccountDto.operations] of the list endpoint (which
 * carries only non-terminal ones), and as the body of the create and sign responses.
 *
 * There is deliberately no `digest` field anywhere in this hierarchy, and one must never appear: the EIP-712
 * digest is computed locally from [safeTx], so a compromised backend cannot substitute what is signed. If the
 * backend ever sends one, Moshi drops it here and no Kotlin code can reach it.
 *
 * @property operationId uuid, the path parameter of the sign and reject endpoints
 * @property chainId     the EVM network the operation lives in — one non-terminal operation per (account, chain)
 * @property safeTx      the full transaction every member signs
 * @property status      `awaiting_signatures` | `pending` | `executed` | `failed`. Raw string: mapping (and
 * tolerance to values a newer backend adds) is the data layer's job, a parse must never fail on it

 * @property deadline    reserved for swap operations; always `null` for a transfer
 * @property signatures  only the members who responded, with their decision; the rest of the roster is drawn
 * from [JointAccountDto.members]
 */
@JsonClass(generateAdapter = true)
data class JointOperationDto(
    @Json(name = "operationId") val operationId: String,
    @Json(name = "chainId") val chainId: Long,
    @Json(name = "safeTx") val safeTx: SafeTxDto,
    @Json(name = "status") val status: String,
    @Json(name = "initiator") val initiator: String? = null,
    @Json(name = "deadline") val deadline: String? = null,
    @Json(name = "signatures") val signatures: List<SignatureDto> = emptyList(),
) {

    /**

     */
    @JsonClass(generateAdapter = true)
    data class SafeTxDto(
        @Json(name = "to") val to: String,
        @Json(name = "value") val value: String,
        @Json(name = "data") val data: String,
        @Json(name = "operation") val operation: Int,
        @Json(name = "safeTxGas") val safeTxGas: String,
        @Json(name = "baseGas") val baseGas: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "gasToken") val gasToken: String,
        @Json(name = "refundReceiver") val refundReceiver: String,
        @Json(name = "nonce") val nonce: String,
    )

    /**
     * @property address the responding member's owner address
     * @property state   `signed` | `declined`. Raw string, same reasoning as [JointOperationDto.status]
     * @property at      when the member responded, ISO-8601
     */
    @JsonClass(generateAdapter = true)
    data class SignatureDto(
        @Json(name = "address") val address: String,
        @Json(name = "state") val state: String,
        @Json(name = "at") val at: String? = null,
    )
}