package com.tangem.features.tangempay.eligibility.country

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.visa.error.VisaError
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockResidencySource @Inject constructor() {

    suspend fun getCountries(): List<CountryOfResidence> {
        delay(LOADING_DELAY_MS)
        return Locale.getISOCountries()
            .map { code ->
                CountryOfResidence(
                    code = code,
                    name = Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH),
                    flagUrl = FLAG_URL_TEMPLATE.format(code.lowercase()),
                    hasStates = code in COUNTRIES_WITH_STATES,
                )
            }
            .sortedBy(CountryOfResidence::name)
    }

    fun getSuggestedCountryCodes(): List<String> = listOfNotNull(Locale.getDefault().country.takeIf(String::isNotEmpty))

    suspend fun getStates(countryCode: String): List<StateOfResidence> {
        delay(LOADING_DELAY_MS)
        return when (countryCode) {
            US_COUNTRY_CODE -> US_STATES.map { (code, name) -> StateOfResidence(code = code, name = name) }
            else -> emptyList()
        }
    }

    fun getSuggestedStateCodes(countryCode: String): List<String> = when (countryCode) {
        US_COUNTRY_CODE -> SUGGESTED_US_STATE_CODES
        else -> emptyList()
    }

    suspend fun checkEligibility(): Either<VisaError, Boolean> {
        delay(CHECK_DELAY_MS)
        return false.right()
    }

    private companion object {
        const val LOADING_DELAY_MS = 700L
        const val CHECK_DELAY_MS = 600L
        val SUGGESTED_US_STATE_CODES = listOf("AZ")
        const val FLAG_URL_TEMPLATE = "https://flagcdn.com/w160/%s.png"
        const val US_COUNTRY_CODE = "US"
        val COUNTRIES_WITH_STATES = setOf(US_COUNTRY_CODE)

        val US_STATES = listOf(
            "AL" to "Alabama",
            "AK" to "Alaska",
            "AZ" to "Arizona",
            "AR" to "Arkansas",
            "CA" to "California",
            "CO" to "Colorado",
            "CT" to "Connecticut",
            "DE" to "Delaware",
            "DC" to "District of Columbia",
            "FL" to "Florida",
            "GA" to "Georgia",
            "HI" to "Hawaii",
            "ID" to "Idaho",
            "IL" to "Illinois",
            "IN" to "Indiana",
            "IA" to "Iowa",
            "KS" to "Kansas",
            "KY" to "Kentucky",
            "LA" to "Louisiana",
            "ME" to "Maine",
            "MD" to "Maryland",
            "MA" to "Massachusetts",
            "MI" to "Michigan",
            "MN" to "Minnesota",
            "MS" to "Mississippi",
            "MO" to "Missouri",
            "MT" to "Montana",
            "NE" to "Nebraska",
            "NV" to "Nevada",
            "NH" to "New Hampshire",
            "NJ" to "New Jersey",
            "NM" to "New Mexico",
            "NY" to "New York",
            "NC" to "North Carolina",
            "ND" to "North Dakota",
            "OH" to "Ohio",
            "OK" to "Oklahoma",
            "OR" to "Oregon",
            "PA" to "Pennsylvania",
            "RI" to "Rhode Island",
            "SC" to "South Carolina",
            "SD" to "South Dakota",
            "TN" to "Tennessee",
            "TX" to "Texas",
            "UT" to "Utah",
            "VT" to "Vermont",
            "VA" to "Virginia",
            "WA" to "Washington",
            "WV" to "West Virginia",
            "WI" to "Wisconsin",
            "WY" to "Wyoming",
        )
    }
}