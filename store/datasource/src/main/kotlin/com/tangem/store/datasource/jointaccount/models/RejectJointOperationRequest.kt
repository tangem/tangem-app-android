package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** No signature: the wallet's session identifies the member, and cancel or decline is decided from it */
@JsonClass(generateAdapter = true)
data class RejectJointOperationRequest(
    @Json(name = "payload") val payload: Payload,
) {

    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "operationId") val operationId: String,
        @Json(name = "safeTx") val safeTx: JointOperationDto.SafeTxDto,
    )
}