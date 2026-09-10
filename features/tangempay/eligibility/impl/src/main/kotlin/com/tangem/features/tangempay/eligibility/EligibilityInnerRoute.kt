package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class EligibilityInnerRoute : Route {

    @Serializable
    data object Residency : EligibilityInnerRoute()

    @Serializable
    data class RegionUnavailable(
        val countryCode: String,
        val regionCode: String?,
    ) : EligibilityInnerRoute()
}