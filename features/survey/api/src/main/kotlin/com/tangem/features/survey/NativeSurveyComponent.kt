package com.tangem.features.survey

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/** Full-screen native survey. Closes itself with router.pop() and reports nothing back to the caller. */
interface NativeSurveyComponent : ComposableContentComponent {

    data class Params(
        val surveyId: Long,
        val source: AppRoute.NativeSurvey.Source,
        val stage: String,
        val repeatAfterDays: Int?,
        val userWalletId: UserWalletId,
        val isAlreadyAnswered: Boolean,
    )

    interface Factory : ComponentFactory<Params, NativeSurveyComponent>
}