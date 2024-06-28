package com.tangem.features.details.routing

import android.os.Bundle
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.navigation.AppScreen

// TODO: Remove after [REDACTED_JIRA]
internal sealed class DetailsRoute : Route {

    data class Screen(
        val screen: AppScreen,
        val params: Bundle? = null,
    ) : DetailsRoute()

    data class Url(val url: String) : DetailsRoute()

    data object Feedback : DetailsRoute()

    data object TesterMenu : DetailsRoute()
}