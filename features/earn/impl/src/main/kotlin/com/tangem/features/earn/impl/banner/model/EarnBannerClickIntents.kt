package com.tangem.features.earn.impl.banner.model

import com.tangem.domain.models.earn.EarnProductId

internal interface EarnBannerClickIntents {

    fun onEnableClick(preselected: EarnProductId?)

    fun onBannerClick()
}