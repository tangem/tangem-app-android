package com.tangem.feature.swap.utils

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.presentation.R

internal fun getExpressErrorMessage(expressDataError: ExpressDataError): TextReference {
    return when (expressDataError) {
        is ExpressDataError.SwapsAreUnavailableNowError -> resourceReference(
            id = R.string.express_error_swap_unavailable,
            formatArgs = wrappedList(expressDataError.code),
        )
        is ExpressDataError.ExchangeNotPossibleError -> resourceReference(
            id = R.string.warning_express_pair_unavailable_message,
            formatArgs = wrappedList(expressDataError.code),
        )
        is ExpressDataError.UnknownError -> resourceReference(R.string.common_unknown_error)
        is ExpressDataError.ExchangeProviderNotActiveError,
        is ExpressDataError.ExchangeProviderNotFoundError,
        is ExpressDataError.ExchangeProviderNotAvailableError,
        is ExpressDataError.ExchangeProviderProviderInternalError,
        -> resourceReference(
            id = R.string.express_error_swap_pair_unavailable,
            formatArgs = wrappedList(expressDataError.code),
        )
        else -> resourceReference(R.string.express_error_code, wrappedList(expressDataError.code.toString()))
    }
}

internal fun getExpressErrorTitle(expressDataError: ExpressDataError): TextReference {
    return when (expressDataError) {
        is ExpressDataError.ExchangeNotPossibleError -> resourceReference(
            id = R.string.warning_express_pair_unavailable_title,
            formatArgs = wrappedList(expressDataError.code),
        )
        is ExpressDataError.UnknownError -> resourceReference(R.string.common_error)
        else -> resourceReference(R.string.warning_express_refresh_required_title)
    }
}