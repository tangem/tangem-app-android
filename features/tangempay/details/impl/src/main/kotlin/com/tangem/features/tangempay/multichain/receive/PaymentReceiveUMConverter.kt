package com.tangem.features.tangempay.multichain.receive

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Builds the pay-specific multi-token Receive-assets sheet state ([PaymentReceiveUM]) from the
 * resolved network's tokens and shared deposit address.
 *
 * See [PaymentReceiveModel] for how [Input] is derived from live payment-account status: the model
 * re-resolves the selected network by its stable rawId on every status update, so this converter
 * stays a pure function of already-resolved data.
 */
internal class PaymentReceiveUMConverter(
    private val onCopy: () -> Unit,
    private val onShowQr: () -> Unit,
    private val onShare: () -> Unit,
    private val onDismiss: () -> Unit,
) : Converter<PaymentReceiveUMConverter.Input, PaymentReceiveUM> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    /**
     * @property networkName human-readable network name, e.g. "Solana".
     * @property address the deposit address shared by all [currencies] on [networkName].
     * @property currencies the network's tokens (order preserved for the icon row).
     */
    data class Input(
        val networkName: String,
        val address: String,
        val currencies: List<CryptoCurrency>,
    )

    override fun convert(value: Input): PaymentReceiveUM {
        val tokensLabel = value.currencies.joinToString(separator = ", ") { it.symbol }
        return PaymentReceiveUM(
            warning = resourceReference(
                id = R.string.receive_bottom_sheet_warning_title,
                formatArgs = wrappedList(tokensLabel, value.networkName),
            ),
            tokensOnNetworkLabel = resourceReference(
                id = R.string.receive_bottom_sheet_warning_message_compact,
                formatArgs = wrappedList(tokensLabel, value.networkName),
            ),
            tokens = value.currencies.map(::toTokenIconUM).toPersistentList(),
            address = value.address,
            onCopy = onCopy,
            onShowQr = onShowQr,
            onShare = onShare,
            onDismiss = onDismiss,
        )
    }

    private fun toTokenIconUM(currency: CryptoCurrency): TokenIconUM {
        return TokenIconUM(symbol = currency.symbol, iconState = iconStateConverter.convert(currency))
    }
}