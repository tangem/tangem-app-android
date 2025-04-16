package com.tangem.tap.data

import android.content.Context
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toInt
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
import com.tangem.datasource.local.visa.VisaOtpData
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

    private val secureStorage by lazy {
        AndroidSecureStorage(
            preferences = SecureStorage.createEncryptedSharedPreferences(
                context = applicationContext,
                storageName = "visa_otp_storage",
            ),
        )
    }

    override suspend fun saveOTP(cardId: String, data: VisaOtpData) = withContext(dispatcherProvider.io) {
        secureStorage.store(data.rootOTP, VISA_ROOT_OTP_KEY_PREFIX + cardId)
        secureStorage.store(data.counter.toByteArray(), VISA_OTP_COUNTER_KEY_PREFIX + cardId)
    }

    override suspend fun getOTP(cardId: String): VisaOtpData? = withContext(dispatcherProvider.io) {
        val rootOTP = secureStorage.get(VISA_ROOT_OTP_KEY_PREFIX + cardId) ?: return@withContext null
        val counter = secureStorage.get(VISA_OTP_COUNTER_KEY_PREFIX + cardId)?.toInt() ?: return@withContext null
        VisaOtpData(
            rootOTP = rootOTP,
            counter = counter,
        )
    }

    override suspend fun removeOTP(cardId: String) = withContext(dispatcherProvider.io) {
        secureStorage.delete(VISA_ROOT_OTP_KEY_PREFIX + cardId)
        secureStorage.delete(VISA_OTP_COUNTER_KEY_PREFIX + cardId)
    }

    private companion object {
        const val VISA_ROOT_OTP_KEY_PREFIX = "visa_root_otp_"
        const val VISA_OTP_COUNTER_KEY_PREFIX = "visa_otp_counter_"
    }
}