package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateJointOperationRequest(
    @Json(name = "safeAddress") val safeAddress: String,
    @Json(name = "chainId") val chainId: Long,
    @Json(name = "safeTx") val safeTx: JointOperationDto.SafeTxDto,
    @Json(name = "signature") val signature: String,
)