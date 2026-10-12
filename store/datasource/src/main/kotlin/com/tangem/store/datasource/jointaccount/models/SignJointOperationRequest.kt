package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignJointOperationRequest(
    @Json(name = "safeTx") val safeTx: JointOperationDto.SafeTxDto,
    @Json(name = "signature") val signature: String,
)