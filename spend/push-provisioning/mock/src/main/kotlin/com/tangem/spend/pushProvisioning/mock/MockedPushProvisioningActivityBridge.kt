package com.tangem.spend.pushProvisioning.mock

import android.app.Activity
import android.content.Intent
import com.tangem.spend.pushProvisioning.PushProvisioningActivityBridge
import javax.inject.Inject

internal class MockedPushProvisioningActivityBridge @Inject constructor() : PushProvisioningActivityBridge {

    override fun attach(activity: Activity) = Unit

    override fun detach(activity: Activity) = Unit

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) = Unit
}