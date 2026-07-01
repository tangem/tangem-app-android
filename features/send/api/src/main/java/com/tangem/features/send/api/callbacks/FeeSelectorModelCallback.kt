package com.tangem.features.send.api.callbacks

import com.tangem.features.send.api.entity.FeeSelectorUM

interface FeeSelectorModelCallback {
    fun onFeeResult(feeSelectorUM: FeeSelectorUM)
}