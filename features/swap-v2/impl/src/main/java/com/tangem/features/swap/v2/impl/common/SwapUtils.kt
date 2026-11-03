package com.tangem.features.swap.v2.impl.common

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.R

internal object SwapUtils {
    const val INCREASE_GAS_LIMIT_FOR_DEX = 112 // 12%
    const val INCREASE_GAS_LIMIT_FOR_CEX = 105 // 5%

    /** List of supported provider types in Send with Swap */
    internal val SEND_WITH_SWAP_PROVIDER_TYPES = listOf(
        ExpressProviderType.CEX,
        // ExpressProviderType.DEX, // TODO disable until implemented in Send with Swap
    )

    fun getExpressErrorMessage(
        expressError: ExpressError,
        providerName: String? = null,
        amountCurrency: CryptoCurrency? = null,
    ): TextReference {
        if (providerName != null && expressError is ExpressError.ExchangeProviderAddressBannedByAmlError) {
            return resourceReference(
                id = R.string.express_error_transaction_cannot_be_created,
                formatArgs = wrappedList(providerName, expressError.code),
            )
        }
        return when (expressError) {
            is ExpressError.AmountError.TooSmallError -> getAmountLimitMessage(
                id = R.string.warning_express_too_minimal_amount_title,
                expressError = expressError,
                amountCurrency = amountCurrency,
            )
            is ExpressError.AmountError.TooBigError -> getAmountLimitMessage(
                id = R.string.warning_express_too_maximum_amount_title,
                expressError = expressError,
                amountCurrency = amountCurrency,
            )
            is ExpressError.InternalError -> resourceReference(
                id = R.string.express_error_swap_unavailable,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.ExchangeNotPossibleError -> resourceReference(
                id = if (providerName != null) {
                    R.string.warning_express_pair_unavailable_message_v2
                } else {
                    R.string.warning_express_pair_unavailable_message
                },
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
            else -> getGeneralErrorMessage(expressError)
        }
    }

    private fun getAmountLimitMessage(
        @StringRes id: Int,
        expressError: ExpressError.AmountError,
        amountCurrency: CryptoCurrency?,
    ): TextReference {
        if (amountCurrency == null) return getGeneralErrorMessage(expressError)
        return resourceReference(
            id = id,
            formatArgs = wrappedList(expressError.amount.format { crypto(amountCurrency) }),
        )
    }

    private fun getGeneralErrorMessage(expressError: ExpressError): TextReference {
        return resourceReference(R.string.express_error_code, wrappedList(expressError.code.toString()))
    }

    internal fun getExpressErrorTitle(expressError: ExpressError): TextReference {
        return when (expressError) {
            is ExpressError.ExchangeNotPossibleError -> resourceReference(
                id = R.string.warning_express_pair_unavailable_title,
                formatArgs = wrappedList(expressError.code),
            )
            is ExpressError.UnknownError,
            is ExpressError.AmountError.TooSmallError,
            is ExpressError.AmountError.TooBigError,
            -> resourceReference(R.string.common_error)
            else -> resourceReference(R.string.warning_express_refresh_required_title)
        }
    }
}