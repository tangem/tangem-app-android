package com.tangem.features.collectibles.impl

import com.tangem.core.decompose.navigation.Route

internal sealed interface CollectiblesRoute : Route {

    data object Onboarding : CollectiblesRoute

    data object Stories : CollectiblesRoute

    data object Main : CollectiblesRoute
}