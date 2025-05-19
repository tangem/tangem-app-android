package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaDataForApprove(
    val customerWalletCardId: String?,
    val targetAddress: String,
    val dataToSign: VisaDataToSignByCustomerWallet,
)