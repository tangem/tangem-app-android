package com.tangem.data.onramp.converters.error

import com.tangem.domain.onramp.model.OnrampError
import com.tangem.utils.converter.Converter

internal class OnrampErrorConverter : Converter<String, OnrampError> {

    override fun convert(value: String): OnrampError {
        return OnrampError.UnknownError
    }
}