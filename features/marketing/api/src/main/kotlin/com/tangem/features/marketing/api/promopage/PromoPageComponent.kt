package com.tangem.features.marketing.api.promopage

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface PromoPageComponent : ComposableContentComponent {

    data class Params(val url: String)

    interface Factory : ComponentFactory<Params, PromoPageComponent>
}