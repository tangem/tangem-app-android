package com.tangem.domain.visa.model

class VisaSignedActivationDataByCardWallet(
    val dataToSign: VisaDataToSignByCardWallet,
    val cardWalletAddress: String,
    val rootOTP: String,
    val otpCounter: Int,
    val signature: String,
)