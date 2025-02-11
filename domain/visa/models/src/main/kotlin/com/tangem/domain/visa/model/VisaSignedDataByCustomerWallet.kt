package com.tangem.domain.visa.model

data class VisaSignedDataByCustomerWallet(
    val dataToSign: VisaDataToSignByCustomerWallet,
    val customerWalletAddress: String,
    val signature: String,
)