package com.tangem.feature.swap.domain.models.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import java.math.BigDecimal

/**
 * Domain layer representation of SwapPair data network model.
 *
 * @property from Short information about the token we want to change
 * @property to Short information about the token we want to exchange for
 * @property providers Exchange providers
 */
data class SwapPairLeast(
    val from: LeastTokenInfo,
    val to: LeastTokenInfo,
    val providers: List<SwapProvider>,
)

data class CryptoCurrencySwapInfo(
    val currencyStatus: CryptoCurrencyStatus,
    val providers: List<SwapProvider>,
)

/**
 * Provider that could swap given cryptocurrencies
 *
 * @property providerId provider id
 * @property rateTypes supported rate types
 * @property isRecommended flag that indicates if this provider is recommended
 *
 * Uses to store transaction data in datastore, when extends - should always add default value
 * to support backward compatibility
 */
@JsonClass(generateAdapter = true)
data class SwapProvider(
    @Json(name = "providerId")
    val providerId: String,
    @Json(name = "rateTypes")
    val rateTypes: List<RateType> = emptyList(),
    @Json(name = "name")
    val name: String,
    @Json(name = "type")
    val type: ExchangeProviderType,
    @Json(name = "imageLarge")
    val imageLarge: String,
    @Json(name = "termsOfUse")
    val termsOfUse: String?,
    @Json(name = "privacyPolicy")
    val privacyPolicy: String?,
    @Json(name = "isRecommended")
    val isRecommended: Boolean = false,
    @Json(name = "slippage")
    val slippage: BigDecimal?,
)

@JsonClass(generateAdapter = false)
enum class ExchangeProviderType(val providerName: String) {
    @Json(name = "DEX") DEX("DEX"),

    @Json(name = "CEX") CEX("CEX"),

    @Json(name = "DEX_BRIDGE") DEX_BRIDGE("DEX/Bridge"),
}

/**
 * Rate type.
 *
 * Current implementation contains only float type, fixed will be supported later.
 */
@JsonClass(generateAdapter = false)
enum class RateType {
    @Json(name = "FLOAT") FLOAT,

    @Json(name = "FIXED") FIXED,
}