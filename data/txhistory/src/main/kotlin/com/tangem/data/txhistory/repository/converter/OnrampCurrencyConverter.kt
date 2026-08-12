package com.tangem.data.txhistory.repository.converter

import com.tangem.datasource.local.txhistory.db.entity.express.OnrampCurrencyEntity
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.utils.converter.Converter

/** Maps a persisted [OnrampCurrencyEntity] into the domain [OnrampCurrency]. */
internal class OnrampCurrencyConverter : Converter<OnrampCurrencyEntity, OnrampCurrency> {

    override fun convert(value: OnrampCurrencyEntity): OnrampCurrency {
        return OnrampCurrency(
            name = value.name,
            code = value.code,
            image = value.image,
            precision = value.precision,
            unit = value.unit,
        )
    }
}