package com.tangem.tap.common.analytics.handlers.firebase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tangem.core.analytics.AppInstanceIdProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

internal class FirebaseAppInstanceIdProvider : AppInstanceIdProvider {

    override suspend fun getAppInstanceId(): String? = suspendCancellableCoroutine { continuation ->
        Firebase.analytics.appInstanceId
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener {
                Timber.w("Fail to get appInstanceId")
                continuation.resume(null)
            }
    }

    override fun getAppInstanceIdSync(): String? {
        return try {
            Firebase.analytics.appInstanceId.result
        } catch (e: IllegalStateException) {
            Timber.e(e, "getAppInstanceIdSync")
            null
        }
    }
}