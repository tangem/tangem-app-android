package com.tangem.tap

import android.content.Context
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HuaweiPushNotificationsTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun getToken(): String = withContext(Dispatchers.IO) {
        try {
            val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID_KEY)
            val token = HmsInstanceId.getInstance(context).getToken(appId, TOKEN_REQUEST_MODE)
            Timber.i("Requested token from HuaweiService: $token")
            token
        } catch (e: ApiException) {
            Timber.i("Fetching token from HuaweiService failed cause: ${e.message}")
            ""
        }
    }

    companion object {
        private const val APP_ID_KEY = "client/app_id"
        private const val TOKEN_REQUEST_MODE = "HCM"
    }
}