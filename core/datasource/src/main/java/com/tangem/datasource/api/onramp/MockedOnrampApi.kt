package com.tangem.datasource.api.onramp

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO
import com.tangem.datasource.api.onramp.models.request.OnrampPairsRequest
import com.tangem.datasource.api.onramp.models.response.OnrampDataResponse
import com.tangem.datasource.api.onramp.models.response.OnrampQuoteResponse
import com.tangem.datasource.api.onramp.models.response.OnrampStatusResponse
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampPairDTO
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO

internal class MockedOnrampApi : OnrampApi {
    override suspend fun getCurrencies(): ApiResponse<List<OnrampCurrencyDTO>> = ApiResponse.Success(
        COUNTRIES.map(OnrampCountryDTO::defaultCurrency),
    )

    override suspend fun getCountries(): ApiResponse<List<OnrampCountryDTO>> = ApiResponse.Success(COUNTRIES + RUSSIA)

    override suspend fun getCountryByIp(): ApiResponse<OnrampCountryDTO> = ApiResponse.Success(RUSSIA)

    override suspend fun getPaymentMethods(): ApiResponse<List<PaymentMethodDTO>> = ApiResponse.Success(
        listOf(
            PaymentMethodDTO(id = "google", name = "Google Play", image = ""),
            PaymentMethodDTO(id = "apple", name = "Apple Pay", image = ""),
            PaymentMethodDTO(id = "card", name = "Card", image = ""),
        ),
    )

    override suspend fun getPairs(body: OnrampPairsRequest): ApiResponse<List<OnrampPairDTO>> = ApiResponse.Success(
        listOf(
            OnrampPairDTO(
                fromCurrencyCode = "USD",
                to = OnrampDestinationDTO(contractAddress = "0xcontract_address", network = "ethereum"),
                providers = listOf(),
            ),
        ),
    )

    override suspend fun getQuote(
        fromCurrencyCode: String,
        toContractAddress: String,
        toNetwork: String,
        paymentMethod: String,
        countryCode: String,
        fromAmount: String,
        toDecimals: Int,
        providerId: String,
    ): ApiResponse<OnrampQuoteResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getData(
        fromCurrencyCode: String,
        toContractAddress: String,
        toNetwork: String,
        paymentMethod: String,
        countryCode: String,
        fromAmount: String,
        toDecimals: Int,
        providerId: String,
        toAddress: String,
        redirectUrl: String,
        language: String?,
        theme: String?,
        requestId: String,
    ): ApiResponse<OnrampDataResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getStatus(txId: String): ApiResponse<OnrampStatusResponse> {
        TODO("Not yet implemented")
    }

    private companion object {
        private val RUSSIA = OnrampCountryDTO(
            name = "Russia",
            code = "RU",
            image = "https://hatscripts.github.io/circle-flags/flags/ru.svg",
            alpha3 = "RUS",
            continent = "",
            defaultCurrency = OnrampCurrencyDTO(
                name = "Russian ruble",
                code = "RUB",
                image = "https://hatscripts.github.io/circle-flags/flags/ru.svg",
                precision = 2,
            ),
            onrampAvailable = false,
        )
        private val COUNTRIES = listOf(
            OnrampCountryDTO(
                name = "United States of America",
                code = "USA",
                image = "https://hatscripts.github.io/circle-flags/flags/us.svg",
                alpha3 = "USA",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "US Dollar",
                    code = "USD",
                    image = "https://hatscripts.github.io/circle-flags/flags/us.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
            OnrampCountryDTO(
                name = "Europe Union",
                code = "EU",
                image = "https://hatscripts.github.io/circle-flags/flags/eu.svg",
                alpha3 = "EUR",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "Euro",
                    code = "EUR",
                    image = "https://hatscripts.github.io/circle-flags/flags/eu.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
            OnrampCountryDTO(
                name = "Great Britain",
                code = "GB",
                image = "https://hatscripts.github.io/circle-flags/flags/gb.svg",
                alpha3 = "GB",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "British Pound Sterling",
                    code = "GBP",
                    image = "https://hatscripts.github.io/circle-flags/flags/gb.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
            OnrampCountryDTO(
                name = "CANADA",
                code = "CA",
                image = "https://hatscripts.github.io/circle-flags/flags/ca.svg",
                alpha3 = "CA",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "Canadian Dollar",
                    code = "CAD",
                    image = "https://hatscripts.github.io/circle-flags/flags/ca.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
            OnrampCountryDTO(
                name = "Hon Kong",
                code = "HK",
                image = "https://hatscripts.github.io/circle-flags/flags/hk.svg",
                alpha3 = "HK",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "Hon Kong Dollar",
                    code = "HKD",
                    image = "https://hatscripts.github.io/circle-flags/flags/hk.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
            OnrampCountryDTO(
                name = "Australia",
                code = "AU",
                image = "https://hatscripts.github.io/circle-flags/flags/au.svg",
                alpha3 = "AU",
                continent = "",
                defaultCurrency = OnrampCurrencyDTO(
                    name = "Australian Dollar",
                    code = "AUD",
                    image = "https://hatscripts.github.io/circle-flags/flags/au.svg",
                    precision = 2,
                ),
                onrampAvailable = true,
            ),
        )
    }
}