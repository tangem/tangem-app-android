package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayCampaignComponent : ComposableContentComponent {

    data class Params(val campaignKey: String)

    interface Factory : ComponentFactory<Params, TangemPayCampaignComponent>
}