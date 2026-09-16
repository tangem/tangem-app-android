package com.tangem.tap.common.pushes

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.features.tester.api.TestPushEmitter
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class DefaultTestPushEmitter @Inject constructor(
    private val pushMessageDispatcher: PushMessageDispatcher,
    private val appScope: AppCoroutineScope,
) : TestPushEmitter {

    override fun emit(data: Map<String, String>) {
        val bundle = Bundle().apply {
            data.forEach { (key, value) -> putString(key, value) }
        }
        appScope.launch {
            pushMessageDispatcher.dispatch(RemoteMessage(bundle))
        }
    }
}