package com.tangem.features.send.api.subcomponents.feeSelector.callbacks

import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM

interface FeeSelectorModelCallback {
    fun onFeeResult(feeSelectorUM: FeeSelectorUM)
}