package com.tangem.tap

import android.os.Bundle
import com.huawei.hms.push.HmsMessageService
import timber.log.Timber

class HuaweiPushService : HmsMessageService() {

    override fun onNewToken(token: String?, bundle: Bundle?) {
        super.onNewToken(token, bundle)
        Timber.i("HuaweiPushService: On new token from HuaweiService: $token")
    }

    override fun onTokenError(e: Exception?, bundle: Bundle?) {
        super.onTokenError(e, bundle)
        Timber.i("HuaweiPushService: Fetching token from HuaweiService failed cause: ${e?.message}")
    }
}