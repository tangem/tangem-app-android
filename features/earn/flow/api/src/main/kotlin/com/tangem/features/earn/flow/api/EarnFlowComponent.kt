package com.tangem.features.earn.flow.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.EarnFlowExit

interface EarnFlowComponent : ComposableContentComponent {

    data class Params(
        val product: EarnProduct.Params,
        val flowProduct: EarnFlowProduct,
        val action: EarnFlowAction,
        val source: EarnEntrySource,
        val callback: ModelCallback,
    )

    interface ModelCallback {

        fun onFinished(exit: EarnFlowExit)
    }

    interface Factory : ComponentFactory<Params, EarnFlowComponent>
}