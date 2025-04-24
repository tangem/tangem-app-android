package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaDataToSignByCardWallet(
    val request: VisaCardWalletDataToSignRequest,
    val hashToSign: String,
)

fun VisaDataToSignByCardWallet.sign(rootOTP: String, otpCounter: Int, signature: String) =
    VisaSignedActivationDataByCardWallet(
        dataToSign = this,
        rootOTP = rootOTP,
        otpCounter = otpCounter,
        signature = signature,
    )