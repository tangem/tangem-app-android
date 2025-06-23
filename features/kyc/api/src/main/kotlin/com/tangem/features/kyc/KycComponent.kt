package com.tangem.features.kyc

import com.tangem.core.decompose.context.AppComponentContext

interface KycComponent {

    fun launch()

    interface Factory {
        fun create(appComponentContext: AppComponentContext): KycComponent
    }
}