package com.tangem.tap.data

import android.content.Context
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
import com.tangem.sdk.storage.AndroidSecureStorage
import com.tangem.sdk.storage.createEncryptedSharedPreferences
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVisaOTPStorage @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val dispatcherProvider: CoroutineDispatcherProvider,
) : VisaOTPStorage {

    private val secureStorage = AndroidSecureStorage(
        preferences = SecureStorage.createEncryptedSharedPreferences(
            context = applicationContext,
            storageName = "visa_otp_storage",
        ),
    )

    override suspend fun saveOTP(cardId: String, otp: ByteArray) = withContext(dispatcherProvider.io) {
        secureStorage.store(otp, VISA_OTP_KEY_PREFIX + cardId)
    }

    override suspend fun getOTP(cardId: String): ByteArray? = withContext(dispatcherProvider.io) {
        secureStorage.get(VISA_OTP_KEY_PREFIX + cardId)
    }

    override suspend fun removeOTP(cardId: String) = withContext(dispatcherProvider.io) {
        secureStorage.delete(VISA_OTP_KEY_PREFIX + cardId)
    }

    private companion object {
        const val VISA_OTP_KEY_PREFIX = "visa_otp_"
    }
}