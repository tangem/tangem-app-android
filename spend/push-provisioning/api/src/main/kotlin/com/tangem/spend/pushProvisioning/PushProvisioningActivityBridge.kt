package com.tangem.spend.pushProvisioning

import android.app.Activity
import android.content.Intent

interface PushProvisioningActivityBridge {

    fun attach(activity: Activity)

    fun detach(activity: Activity)

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}