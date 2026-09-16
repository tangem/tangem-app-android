package com.tangem.data.pay.util

import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.spend.datasource.pay.models.response.EligibilityCheckResponse
import com.tangem.spend.datasource.pay.models.response.GeoCountriesResponse
import com.tangem.spend.datasource.pay.models.response.GeoRegionsResponse
import com.tangem.utils.converter.Converter

internal object TangemPayCountryConverter : Converter<GeoCountriesResponse.Country, TangemPayCountry> {

    override fun convert(value: GeoCountriesResponse.Country): TangemPayCountry {
        return TangemPayCountry(
            code = value.code,
            name = value.name,
            flagUrl = value.flagUrl,
            isSuggested = value.isSuggested == true,
            hasRegions = value.hasRegions == true,
        )
    }
}

internal object TangemPayRegionConverter : Converter<GeoRegionsResponse.Region, TangemPayRegion> {

    override fun convert(value: GeoRegionsResponse.Region): TangemPayRegion {
        return TangemPayRegion(
            code = value.code,
            name = value.name,
            isSuggested = value.isSuggested == true,
        )
    }
}

internal object TangemPayEligibilityConverter : Converter<EligibilityCheckResponse.Result, TangemPayEligibility> {

    override fun convert(value: EligibilityCheckResponse.Result): TangemPayEligibility {
        return TangemPayEligibility(
            isAvailable = value.isAvailable,
            country = value.country,
            region = value.region,
        )
    }
}