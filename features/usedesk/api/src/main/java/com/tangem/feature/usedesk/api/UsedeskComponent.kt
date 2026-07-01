package com.tangem.feature.usedesk.api

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface UsedeskComponent : ComposableContentComponent {

    data class Params(
        /** User wallet id (hex string) sent to Usedesk as the client email to identify the user. */
        val userWalletId: String? = null,
        /**
         * Source of the chat opening. Drives the analytics source and the Usedesk additional fields,
         * which the component maps internally (e.g. [AnalyticsParam.ScreensSources.Swap] -> `swap`).
         */
        val source: AnalyticsParam.ScreensSources = AnalyticsParam.ScreensSources.Settings,
        /** Optional message sent to the chat on behalf of the user right after initialization. */
        val prefilledMessage: String? = null,
    )

    interface Factory : ComponentFactory<Params, UsedeskComponent>
}