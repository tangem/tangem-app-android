package com.tangem.features.swap.v2.impl.chooseprovider.model.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderState
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderState.AdditionalBadge
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.converter.Converter

@Deprecated("Remove with new design")
internal class SwapProviderStateConverter(
    private val cryptoCurrency: CryptoCurrency,
    private val selectedProvider: ExpressProvider,
    private val isNeedBestRateBadge: Boolean,
    private val needApplyFCARestrictions: Boolean,
) : Converter<SwapQuoteUM, SwapProviderState> {

    override fun convert(value: SwapQuoteUM): SwapProviderState {
        return when (value) {
            is SwapQuoteUM.Content -> value.convertToContent()
            is SwapQuoteUM.Error -> value.convertToErrorContent()
            is SwapQuoteUM.Allowance,
            SwapQuoteUM.Empty,
            SwapQuoteUM.Loading,
            -> SwapProviderState.Empty
        }
    }

    private fun SwapQuoteUM.Content.convertToContent(): SwapProviderState {
        val isBestRate = when (diffPercent) {
            SwapQuoteUM.Content.DifferencePercent.Best -> true
            else -> false
        }

        val additionalBadge = when {
            needApplyFCARestrictions && provider.isRestrictedByFCA() -> AdditionalBadge.FCAWarningList
            isNeedBestRateBadge && isBestRate -> AdditionalBadge.BestTrade
            else -> AdditionalBadge.Empty
        }

        return SwapProviderState.Content(
            name = provider.name,
            iconUrl = provider.imageLarge,
            type = provider.type.typeName,
            subtitle = quoteAmountValue,
            additionalBadge = additionalBadge,
            diffPercent = diffPercent,
            isSelected = provider == selectedProvider,
        )
    }

    private fun SwapQuoteUM.Error.convertToErrorContent(): SwapProviderState {
        val additionalBadge = when {
            needApplyFCARestrictions && provider.isRestrictedByFCA() -> AdditionalBadge.FCAWarningList
            else -> AdditionalBadge.Empty
        }

        return SwapProviderState.Content(
            name = provider.name,
            iconUrl = provider.imageLarge,
            type = provider.type.typeName,
            subtitle = when (val error = expressError) {
                is ExpressError.AmountError.TooSmallError -> resourceReference(
                    id = R.string.express_provider_min_amount,
                    formatArgs = wrappedList(
                        error.amount.format { crypto(cryptoCurrency) },
                    ),
                )
                is ExpressError.AmountError.NotEnoughAllowanceError,
                is ExpressError.AmountError.TooBigError,
                -> resourceReference(
                    id = R.string.express_provider_max_amount,
                    formatArgs = wrappedList(
                        error.amount.format { crypto(cryptoCurrency) },
                    ),
                )
                else -> TextReference.EMPTY
            },
            additionalBadge = additionalBadge,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
            isSelected = false,
        )
    }
}