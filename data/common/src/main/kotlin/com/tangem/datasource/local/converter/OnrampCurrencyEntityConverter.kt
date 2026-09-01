package com.tangem.datasource.local.converter

import com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCurrencyEntity

/** Maps an [OnrampCurrencyDTO] API response into its persisted [OnrampCurrencyEntity]. */
fun OnrampCurrencyDTO.toEntity(): OnrampCurrencyEntity {
    return OnrampCurrencyEntity(
        code = code,
        name = name,
        image = image,
        precision = precision,
        unit = unit ?: code,
    )
}