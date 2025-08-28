package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaDataToSignByCustomerWallet(
    val hashToSign: String,
    val request: VisaCustomerWalletDataToSignRequest? = null,
)

fun VisaDataToSignByCustomerWallet.sign(signature: String, customerWalletAddress: String) =
    VisaSignedDataByCustomerWallet(
        dataToSign = this,
        customerWalletAddress = customerWalletAddress,
        signature = signature,
    )