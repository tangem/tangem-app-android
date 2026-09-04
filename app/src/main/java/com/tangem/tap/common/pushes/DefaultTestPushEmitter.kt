package com.tangem.tap.common.pushes

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.notifications.TestPushEmitter
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

internal class DefaultTestPushEmitter @Inject constructor(
    private val pushMessageDispatcher: PushMessageDispatcher,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : TestPushEmitter {

    override fun emit(data: Map<String, String>) {
        val bundle = Bundle().apply {
            putString(MESSAGE_ID_KEY, "test-push-${UUID.randomUUID()}")
            data.forEach { (key, value) -> putString(key, value) }
        }
        appScope.launch(dispatchers.io) {
            pushMessageDispatcher.dispatch(RemoteMessage(bundle))
        }
    }

    private companion object {
        const val MESSAGE_ID_KEY = "google.message_id"
    }
}