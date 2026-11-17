package com.tangem.features.tokenreceive.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.Asset
import com.tangem.domain.models.TokenReceiveType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import com.tangem.lib.crypto.receiveNetworkLabel
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal class ReceiveScreenStateFactory(
    private val cryptoCurrency: CryptoCurrency,
    private val asset: Asset,
    private val type: TokenReceiveType,
    private val tokenName: String,
    private val shouldShowMemoDisclaimer: Boolean,
    private val iconState: CurrencyIconState,
    private val warnings: ImmutableList<NotificationUM>,
) {

    private val networkName = cryptoCurrency.network.name
    private val networkLabel = cryptoCurrency.receiveNetworkLabel()

    fun create(
        addresses: List<ReceiveAddress>,
        onCopyAddressClick: (ReceiveAddress) -> Unit,
        onCopyEnsClick: (ReceiveAddress) -> Unit,
    ): ReceiveScreenUM {
        val address = addresses.first { it.type is ReceiveAddress.Type.Primary }
        val ens = addresses.firstOrNull { it.type == ReceiveAddress.Type.Ens }
        return ReceiveScreenUM(
            title = title(),
            subtitle = subtitle(),
            iconState = iconState,
            address = address.value,
            ensName = ens?.value,
            extrasHint = receiveExtrasHint(
                shouldShowMemoDisclaimer = shouldShowMemoDisclaimer,
                transactionExtrasType = cryptoCurrency.network.transactionExtrasType,
            ),
            networkNotice = networkNotice(),
            warnings = warnings,
            onCopyAddressClick = { onCopyAddressClick(address) },
            onCopyEnsClick = { ens?.let(onCopyEnsClick) },
        )
    }

    private fun title(): TextReference {
        val isCurrency = type is TokenReceiveType.Default && asset == Asset.Currency
        return stringReference(if (isCurrency) "${cryptoCurrency.name} • ${cryptoCurrency.symbol}" else tokenName)
    }

    private fun subtitle(): TextReference = if (networkLabel != null) {
        resourceReference(R.string.receive_network_subtitle, wrappedList(networkName, networkLabel))
    } else {
        resourceReference(R.string.domain_receive_assets_onboarding_network_name, wrappedList(networkName))
    }

    private fun networkNotice(): TextReference = if (networkLabel != null) {
        resourceReference(
            id = R.string.receive_send_only_on_network_with_label,
            formatArgs = wrappedList(tokenName, networkName, networkLabel),
        )
    } else {
        resourceReference(R.string.receive_send_only_on_network, wrappedList(tokenName, networkName))
    }
}