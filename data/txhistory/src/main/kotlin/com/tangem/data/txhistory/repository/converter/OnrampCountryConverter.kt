package com.tangem.data.txhistory.repository.converter

import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCountryEntity
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.utils.converter.Converter

/** Maps a persisted [OnrampCountryEntity] into the domain [OnrampCountry]. */
internal class OnrampCountryConverter : Converter<OnrampCountryEntity, OnrampCountry> {

    override fun convert(value: OnrampCountryEntity): OnrampCountry {
        return OnrampCountry(
            id = "${value.alpha3}-${value.name}",
            name = value.name,
            code = value.code,
            image = value.image,
            alpha3 = value.alpha3,
            continent = value.continent,
            defaultCurrency = OnrampCurrency(
                name = value.defaultCurrency.name,
                code = value.defaultCurrency.code,
                image = value.defaultCurrency.image,
                precision = value.defaultCurrency.precision,
                unit = value.defaultCurrency.unit,
            ),
            onrampAvailable = value.isOnrampAvailable,
        )
    }
}