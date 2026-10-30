package com.tangem.features.marketing.api.concierge

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * "Join the waitlist" sheet: a short demand survey shown where Concierge isn't available yet. The sheet is shown
 * while the component is in composition and closes itself by popping its router.
 */
interface ConciergeDemandSurveyComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, ConciergeDemandSurveyComponent>
}