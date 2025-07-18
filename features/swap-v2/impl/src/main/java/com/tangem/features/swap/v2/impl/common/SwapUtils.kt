package com.tangem.features.swap.v2.impl.common

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.express.models.ExpressError
import com.tangem.features.swap.v2.impl.R

internal object SwapUtils {
    const val INCREASE_GAS_LIMIT_FOR_DEX = 112 // 12%
    const val INCREASE_GAS_LIMIT_FOR_CEX = 105 // 5%

    fun getExpressErrorMessage(expressError: ExpressError): TextReference {
        return when (expressError) {
            is ExpressError.InternalError -> resourceReference(
                id = R.string.express_error_swap_unavailable,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.ExchangeNotPossibleError -> resourceReference(
                id = R.string.warning_express_pair_unavailable_message,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.UnknownError -> resourceReference(R.string.common_unknown_error)
            is ExpressError.ProviderNotActiveError,
            is ExpressError.ProviderNotFoundError,
            is ExpressError.ProviderNotAvailableError,
            is ExpressError.ProviderInternalError,
            -> resourceReference(
                id = R.string.express_error_swap_pair_unavailable,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.ProviderDifferentAmountError -> resourceReference(
                R.string.express_error_provider_amount_roundup,
                formatArgs = wrappedList(
                    expressError.code,
                    expressError.fromProviderAmount.format { simple(decimals = expressError.decimals) },
                ),
            )
            else -> resourceReference(R.string.express_error_code, wrappedList(expressError.code.toString()))
        }
    }

    internal fun getExpressErrorTitle(expressError: ExpressError): TextReference {
        return when (expressError) {
            is ExpressError.ExchangeNotPossibleError -> resourceReference(
                id = R.string.warning_express_pair_unavailable_title,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.UnknownError -> resourceReference(R.string.common_error)
            else -> resourceReference(R.string.warning_express_refresh_required_title)
        }
    }
}