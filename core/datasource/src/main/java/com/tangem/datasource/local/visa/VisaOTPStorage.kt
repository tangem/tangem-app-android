package com.tangem.datasource.local.visa

interface VisaOTPStorage {

    suspend fun saveOTP(cardId: String, otp: ByteArray)

    suspend fun getOTP(cardId: String): ByteArray?

    suspend fun removeOTP(cardId: String)
}

suspend fun VisaOTPStorage.hasSavedOTP(cardId: String): Boolean = getOTP(cardId) != null