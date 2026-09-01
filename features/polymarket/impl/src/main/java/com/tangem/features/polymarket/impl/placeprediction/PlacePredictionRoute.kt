package com.tangem.features.polymarket.impl.placeprediction

import com.tangem.core.decompose.navigation.Route

/**
 * Steps of the place-prediction flow, an inner stack owned by [PlacePredictionComponent].
 *
 * `serializer = null` is used in the stack, so no `@Serializable` is required here.
 */
internal sealed interface PlacePredictionRoute : Route {

    data object Amount : PlacePredictionRoute

    data object Summary : PlacePredictionRoute

    /** Terminal: must be entered via `replaceAll`, so back never returns to [Summary]. */
    data object Status : PlacePredictionRoute
}