package com.tangem.domain.visa.model

data class VisaDataToSignByCustomerWallet(val hashToSign: String)

fun VisaDataToSignByCustomerWallet.sign(signature: String, customerWalletAddress: String) =
    VisaSignedDataByCustomerWallet(
        dataToSign = this,
        customerWalletAddress = customerWalletAddress,
        signature = signature,
    )