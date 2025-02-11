package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaDataToSignByCustomerWallet(
    val request: VisaCustomerWalletDataToSignRequest,
    val hashToSign: String,
)

fun VisaDataToSignByCustomerWallet.sign(signature: String, customerWalletAddress: String) =
    VisaSignedDataByCustomerWallet(
        dataToSign = this,
        customerWalletAddress = customerWalletAddress,
        signature = signature,
    )