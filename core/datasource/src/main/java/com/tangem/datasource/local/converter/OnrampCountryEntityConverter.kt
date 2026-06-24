package com.tangem.datasource.local.converter

import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCountryEntity

/** Maps an [OnrampCountryDTO] API response into its persisted [OnrampCountryEntity]. */
fun OnrampCountryDTO.toEntity(): OnrampCountryEntity {
    return OnrampCountryEntity(
        code = code,
        name = name,
        image = image,
        alpha3 = alpha3,
        continent = continent,
        isOnrampAvailable = onrampAvailable,
        defaultCurrency = OnrampCountryEntity.CurrencyEmbedded(
            name = defaultCurrency.name,
            code = defaultCurrency.code,
            image = defaultCurrency.image,
            precision = defaultCurrency.precision,
            unit = defaultCurrency.unit ?: defaultCurrency.code,
        ),
    )
}