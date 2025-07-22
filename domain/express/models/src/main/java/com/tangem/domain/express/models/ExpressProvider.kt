package com.tangem.domain.express.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Express provider data model
 *
 * @property providerId     provider id
 * @property rateTypes      supported rate types
 * @property name           provider name
 * @property type           provider type
 * @property imageLarge     provider large logo image
 * @property termsOfUse     terms of use link
 * @property privacyPolicy  privacy policy link
 * @property isRecommended  flag that indicates if this provider is recommended
 * @property slippage       provider slippage
 *
 * Uses to store transaction data in datastore, when extends - should always add default value
 * to support backward compatibility
 */
@JsonClass(generateAdapter = true)
data class ExpressProvider(
    @Json(name = "providerId")
    val providerId: String,
    @Json(name = "rateTypes")
    val rateTypes: List<ExpressRateType> = emptyList(),
    @Json(name = "name")
    val name: String,
    @Json(name = "type")
    val type: ExpressProviderType,
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