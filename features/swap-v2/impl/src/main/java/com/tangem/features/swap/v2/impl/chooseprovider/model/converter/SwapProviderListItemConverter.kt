package com.tangem.features.swap.v2.impl.chooseprovider.model.converter

import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderListItem
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.converter.Converter

internal class SwapProviderListItemConverter(
    private val cryptoCurrency: CryptoCurrency,
    private val selectedProvider: ExpressProvider,
    private val needApplyFCARestrictions: Boolean,
) : Converter<SwapQuoteUM, SwapProviderListItem?> {
    override fun convert(value: SwapQuoteUM): SwapProviderListItem? {
        val provider = value.provider ?: return null

        return SwapProviderListItem(
            providerUM = ProviderChooseUM(
                title = stringReference(provider.name),
                subtitle = stringReference(provider.type.typeName),
                infoText = when (value) {
                    is SwapQuoteUM.Content -> value.quoteAmountValue
                    else -> TextReference.EMPTY
                },
                iconUrl = provider.imageLarge,
                isSelected = provider.providerId == selectedProvider.providerId,
                extraUM = getExtras(value = value),
                labelUM = getLabel(value = value),
            ),
            quote = value,
        )
    }

    private fun getExtras(value: SwapQuoteUM) = when (value) {
        is SwapQuoteUM.Allowance -> ProviderChooseUM.ExtraUM.Action(
            text = resourceReference(R.string.express_provider_permission_needed),
        )
        is SwapQuoteUM.Error -> {
            ProviderChooseUM.ExtraUM.Error(
                when (val error = value.expressError) {
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
            )
        }
        is SwapQuoteUM.Content -> if (needApplyFCARestrictions && value.provider.isRestrictedByFCA()) {
            ProviderChooseUM.ExtraUM.Action(
                text = resourceReference(R.string.express_provider_fca_warning_list),
            )
        } else {
            ProviderChooseUM.ExtraUM.Empty
        }
        SwapQuoteUM.Empty,
        SwapQuoteUM.Loading,
        -> ProviderChooseUM.ExtraUM.Empty
    }

    private fun getLabel(value: SwapQuoteUM) = when (val diff = (value as? SwapQuoteUM.Content)?.diffPercent) {
        SwapQuoteUM.Content.DifferencePercent.Best -> LabelUM.Info(
            AuditLabelUM(
                text = resourceReference(R.string.express_provider_best_rate),
                type = AuditLabelUM.Type.Info,
            ),
        )
        is SwapQuoteUM.Content.DifferencePercent.Diff -> LabelUM.Text(diff.percent)
        else -> null
    }
}