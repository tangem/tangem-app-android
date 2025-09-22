package com.tangem.domain.pay.model

private const val APPROVED_KYC_STATUS = "APPROVED"
private const val ACTIVE_PI_STATUS = "active"

data class CustomerInfo(
    val productInstance: ProductInstance?,
    val kycStatus: String?,
) {

    fun isKycApproved() = kycStatus == APPROVED_KYC_STATUS

    fun isProductInstanceActive() = productInstance?.status == ACTIVE_PI_STATUS
}

data class ProductInstance(
    val id: String,
    val status: String,
)