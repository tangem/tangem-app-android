package com.tangem.features.kyc

import com.tangem.core.decompose.context.AppComponentContext

interface KycComponent {

    fun launch(params: Params)

    interface Factory {
        fun create(appComponentContext: AppComponentContext): KycComponent
    }

    data class Params(
        val targetAddress: String,
        val cardId: String,
    )
}