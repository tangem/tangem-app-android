package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.R
import com.tangem.utils.converter.Converter

internal class SwapAmountErrorConverter(
    private val cryptoCurrency: CryptoCurrency,
) : Converter<ExpressError, TextReference?> {

    override fun convert(value: ExpressError): TextReference? = when (value) {
        is ExpressError.AmountError.TooSmallError -> resourceReference(
            R.string.express_provider_min_amount,
            wrappedList(value.amount.format { crypto(cryptoCurrency = cryptoCurrency) }),
        )
        is ExpressError.AmountError.TooBigError -> resourceReference(
            R.string.express_provider_max_amount,
            wrappedList(value.amount.format { crypto(cryptoCurrency = cryptoCurrency) }),
        )
        else -> null
    }
}