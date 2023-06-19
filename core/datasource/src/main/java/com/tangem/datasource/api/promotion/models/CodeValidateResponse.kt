package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
 * @author Anton Zhilenkov on 05.06.2023.
 */
data class CodeValidateResponse(
    @Json(name = "valid") val valid: Boolean?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse()
