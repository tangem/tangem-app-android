package com.tangem.data.pay.repository

import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion

/**
 * Countries and regions the mock residency repository answers with. Only the countries listed in [regions] report
 * [TangemPayCountry.hasRegions], so the picker exercises both the one-step and the country-then-region flows.
 */
internal object MockTangemPayResidencySeed {

    private const val FLAG_URL_TEMPLATE = "https://flagcdn.com/w80/%s.png"

    val regions: Map<String, List<TangemPayRegion>> = mapOf(
        "US" to usStates(),
        "CA" to canadianProvinces(),
        "AU" to australianStates(),
    )

    val countries: List<TangemPayCountry> = listOf(
        country(code = "US", name = "United States", isSuggested = true),
        country(code = "CA", name = "Canada", isSuggested = true),
        country(code = "GB", name = "United Kingdom", isSuggested = true),
        country(code = "AE", name = "United Arab Emirates"),
        country(code = "AU", name = "Australia"),
        country(code = "BR", name = "Brazil"),
        country(code = "CH", name = "Switzerland"),
        country(code = "DE", name = "Germany"),
        country(code = "ES", name = "Spain"),
        country(code = "FR", name = "France"),
        country(code = "IT", name = "Italy"),
        country(code = "JP", name = "Japan"),
        country(code = "KZ", name = "Kazakhstan"),
        country(code = "MC", name = "Monaco"),
        country(code = "NL", name = "Netherlands"),
        country(code = "PL", name = "Poland"),
        country(code = "PT", name = "Portugal"),
        country(code = "SG", name = "Singapore"),
        country(code = "TR", name = "Türkiye"),
        country(code = "UZ", name = "Uzbekistan"),
    )

    private fun country(code: String, name: String, isSuggested: Boolean = false) = TangemPayCountry(
        code = code,
        name = name,
        flagUrl = FLAG_URL_TEMPLATE.format(code.lowercase()),
        isSuggested = isSuggested,
        hasRegions = code in regions,
    )

    private fun usStates(): List<TangemPayRegion> = listOf(
        region(code = "CA", name = "California", isSuggested = true),
        region(code = "NY", name = "New York", isSuggested = true),
        region(code = "TX", name = "Texas", isSuggested = true),
        region(code = "AL", name = "Alabama"),
        region(code = "AK", name = "Alaska"),
        region(code = "AZ", name = "Arizona"),
        region(code = "CO", name = "Colorado"),
        region(code = "CT", name = "Connecticut"),
        region(code = "DE", name = "Delaware"),
        region(code = "FL", name = "Florida"),
        region(code = "GA", name = "Georgia"),
        region(code = "HI", name = "Hawaii"),
        region(code = "IL", name = "Illinois"),
        region(code = "MA", name = "Massachusetts"),
        region(code = "MI", name = "Michigan"),
        region(code = "NC", name = "North Carolina"),
        region(code = "NJ", name = "New Jersey"),
        region(code = "NV", name = "Nevada"),
        region(code = "OH", name = "Ohio"),
        region(code = "OR", name = "Oregon"),
        region(code = "PA", name = "Pennsylvania"),
        region(code = "UT", name = "Utah"),
        region(code = "VA", name = "Virginia"),
        region(code = "WA", name = "Washington"),
    )

    private fun canadianProvinces(): List<TangemPayRegion> = listOf(
        region(code = "ON", name = "Ontario", isSuggested = true),
        region(code = "QC", name = "Quebec", isSuggested = true),
        region(code = "AB", name = "Alberta"),
        region(code = "BC", name = "British Columbia"),
        region(code = "MB", name = "Manitoba"),
        region(code = "NB", name = "New Brunswick"),
        region(code = "NL", name = "Newfoundland and Labrador"),
        region(code = "NS", name = "Nova Scotia"),
        region(code = "PE", name = "Prince Edward Island"),
        region(code = "SK", name = "Saskatchewan"),
    )

    private fun australianStates(): List<TangemPayRegion> = listOf(
        region(code = "NSW", name = "New South Wales", isSuggested = true),
        region(code = "VIC", name = "Victoria"),
        region(code = "QLD", name = "Queensland"),
        region(code = "SA", name = "South Australia"),
        region(code = "TAS", name = "Tasmania"),
        region(code = "WA", name = "Western Australia"),
    )

    private fun region(code: String, name: String, isSuggested: Boolean = false) = TangemPayRegion(
        code = code,
        name = name,
        isSuggested = isSuggested,
    )
}