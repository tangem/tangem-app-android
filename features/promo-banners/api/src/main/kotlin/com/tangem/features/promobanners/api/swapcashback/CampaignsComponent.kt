package com.tangem.features.promobanners.api.swapcashback

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/**

 * once at app startup (hence [Unit] params) and reacts to campaign requests coming through the
 * promo-campaigns bus, not to navigation.
 */
interface CampaignsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, CampaignsComponent>
}