package com.tangem.tap

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.tangem.google.GoogleServicesHelper
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class HuaweiPushNotificationsTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
) : PushNotificationsTokenProvider {

    override suspend fun getToken(): String {
        val isGoogleServicesAvailable = GoogleServicesHelper.checkGoogleServicesAvailability(context)
        return if (isGoogleServicesAvailable) {
            try {
                FirebaseMessaging.getInstance().token.await()
            } catch (ex: Exception) {
                Timber.e(ex)
                ""
            }
        } else {
            withContext(coroutineDispatcherProvider.io) {
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
        }
    }

    companion object {
        private const val APP_ID_KEY = "client/app_id"
        private const val TOKEN_REQUEST_MODE = "HCM"
    }
}