package com.tangem.features.send.v2.api.callbacks

import com.tangem.features.send.v2.api.entity.FeeSelectorUM

interface FeeSelectorModelCallback {
    fun onFeeResult(feeSelectorUM: FeeSelectorUM)
}