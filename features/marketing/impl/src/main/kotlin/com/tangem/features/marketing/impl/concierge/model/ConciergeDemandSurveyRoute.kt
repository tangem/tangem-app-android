package com.tangem.features.marketing.impl.concierge.model

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

/** Steps of the demand survey, stacked inside its one modal. */
@Serializable
internal enum class ConciergeDemandSurveyRoute : Route {
    Survey,

    /** Replaces the whole stack, so back and close end the flow instead of returning to the sent survey. */
    Success,
}