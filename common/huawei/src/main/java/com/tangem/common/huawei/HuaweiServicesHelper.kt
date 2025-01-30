package com.tangem.common.huawei

import android.content.Context
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

object HuaweiServicesHelper {
    fun checkHuaweiServicesAvailability(context: Context): Boolean {
        val huaweiApiAvailability = HuaweiApiAvailability.getInstance()
        val status = huaweiApiAvailability.isHuaweiMobileServicesAvailable(context)

        return status == ConnectionResult.SUCCESS
    }
}