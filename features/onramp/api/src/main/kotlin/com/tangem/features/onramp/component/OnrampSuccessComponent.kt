package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnrampSuccessComponent : ComposableContentComponent {

    data class Params(val txId: String)

    interface Factory : ComponentFactory<Params, OnrampSuccessComponent>
}