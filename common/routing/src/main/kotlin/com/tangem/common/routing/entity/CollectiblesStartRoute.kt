package com.tangem.common.routing.entity

import kotlinx.serialization.Serializable

/** Which screen collectibles open on — carried by [com.tangem.common.routing.AppRoute.Collectibles]. */
@Serializable
sealed interface CollectiblesStartRoute {

    /** The wallet has no collectibles account yet. */
    @Serializable
    data object Onboarding : CollectiblesStartRoute

    /** The wallet's account is already created. */
    @Serializable
    data object Main : CollectiblesStartRoute
}