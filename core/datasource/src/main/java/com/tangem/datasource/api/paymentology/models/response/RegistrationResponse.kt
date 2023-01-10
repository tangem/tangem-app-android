package com.tangem.datasource.api.paymentology.models.response

import com.squareup.moshi.Json

data class RegistrationResponse(
    @Json(name = "results") val results: List<Item> = listOf(),
    @Json(name = "success") override val success: Boolean,
    @Json(name = "error") override val error: String?,
    @Json(name = "errorCode") override val errorCode: Int?,
) : ResponseError {

    data class Item(
        @Json(name = "CID") val cardId: String,
        @Json(name = "passed") val passed: Boolean?,
        @Json(name = "active") val active: Boolean?,
        @Json(name = "pin_set") val pinSet: Boolean?,
        @Json(name = "blockchain_init") val blockchainInit: Boolean?,
        @Json(name = "kyc_passed") val kycPassed: Boolean?,
        @Json(name = "kyc_provider") val kycProvider: String?,
        @Json(name = "kyc_date") val kycDate: String?,
        @Json(name = "kyc_status") val kycStatus: KYCStatus?,
        @Json(name = "disabled_by_admin") val disabledByAdmin: Boolean?,
        @Json(name = "error") val error: String?,
    )
}
