package com.tangem.domain.visa.model

class VisaSignedActivationDataByCardWallet(
    val dataToSign: VisaDataToSignByCardWallet,
    val rootOTP: String,
    val otpCounter: Int,
    val signature: String,
)