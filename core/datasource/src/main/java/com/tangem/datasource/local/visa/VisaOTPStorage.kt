package com.tangem.datasource.local.visa

interface VisaOTPStorage {

    suspend fun saveOTP(cardId: String, data: VisaOtpData)

    suspend fun getOTP(cardId: String): VisaOtpData?

    suspend fun removeOTP(cardId: String)
}

class VisaOtpData(
    val rootOTP: ByteArray,
    val counter: Int,
)

suspend fun VisaOTPStorage.hasSavedOTP(cardId: String): Boolean = getOTP(cardId) != null