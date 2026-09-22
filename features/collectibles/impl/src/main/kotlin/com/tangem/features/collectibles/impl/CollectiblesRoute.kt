package com.tangem.features.collectibles.impl

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId

internal sealed interface CollectiblesRoute : Route {

    data class Onboarding(val userWalletId: UserWalletId) : CollectiblesRoute

    data object Stories : CollectiblesRoute

    data class Main(val userWalletId: UserWalletId) : CollectiblesRoute
}