package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RejectJointOperationRequest(
    @Json(name = "payload") val payload: Payload,
    @Json(name = "signature") val signature: String,
) {

    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "operationId") val operationId: String,
        @Json(name = "safeTx") val safeTx: JointOperationDto.SafeTxDto,
    )
}