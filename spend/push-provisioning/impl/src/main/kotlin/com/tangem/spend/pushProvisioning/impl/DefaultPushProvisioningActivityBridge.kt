package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import android.content.Intent
import com.tangem.spend.pushProvisioning.PushProvisioningActivityBridge
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPushProvisioningActivityBridge @Inject constructor(
    private val sdk: MeaWalletGooglePay,
) : PushProvisioningActivityBridge {

    var activity: Activity? = null
        private set

    private var awaitedFlow: Job? = null

    val isAwaitingResult: Boolean
        get() = awaitedFlow?.isActive == true

    fun forwardResultsDuring(flow: Job): Boolean {
        if (isAwaitingResult) return false

        awaitedFlow = flow
        return true
    }

    override fun attach(activity: Activity) {
        this.activity = activity
    }

    override fun detach(activity: Activity) {
        if (this.activity === activity) {
            this.activity = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val activity = activity ?: return
        if (!isAwaitingResult) return

        runCatching {
            sdk.handleOnActivityResult(
                requestCode = requestCode,
                resultCode = resultCode,
                data = data,
                activity = activity,
            )
        }.onFailure {
            TangemLogger.withTag(PUSH_PROVISIONING_LOG_TAG).e("Failed to forward activity result: ${it.describe()}")
        }
    }
}