package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class EligibilityInnerRoute : Route {

    @Serializable
    class Countries : EligibilityInnerRoute()

    @Serializable
    data class Regions(
        val countryCode: String,
        val countryName: String,
    ) : EligibilityInnerRoute()

    @Serializable
    data class RegionUnavailable(
        val countryCode: String,
        val regionCode: String?,
    ) : EligibilityInnerRoute()
}