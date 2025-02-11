package com.tangem.domain.visa.model

data class VisaDataToSignByCardWallet(
    val request: VisaCardWalletDataToSignRequest,
    val hashToSign: String,
)

fun VisaDataToSignByCardWallet.sign(cardWalletAddress: String, rootOTP: String, otpCounter: Int, signature: String) =
    VisaSignedActivationDataByCardWallet(
        dataToSign = this,
        cardWalletAddress = cardWalletAddress,
        rootOTP = rootOTP,
        otpCounter = otpCounter,
        signature = signature,
    )