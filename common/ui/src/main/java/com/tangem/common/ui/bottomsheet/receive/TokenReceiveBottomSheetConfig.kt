package com.tangem.common.ui.bottomsheet.receive

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class TokenReceiveBottomSheetConfig(
    val asset: Asset,
    val network: String,
    val addresses: ImmutableList<AddressModel>,
    val showMemoDisclaimer: Boolean,
    val notifications: ImmutableList<NotificationConfig>,
    val onCopyClick: (String) -> Unit,
    val onShareClick: (String) -> Unit,
) : TangemBottomSheetConfigContent {

    constructor(
        asset: Asset,
        network: Network,
        networkAddress: NetworkAddress,
        showMemoDisclaimer: Boolean,
        customNotifications: ImmutableList<NotificationConfig> = persistentListOf(),
        onCopyClick: (String) -> Unit,
        onShareClick: (String) -> Unit,
    ) : this(
        asset = asset,
        network = network.name,
        addresses = networkAddress.availableAddresses
            .mapToAddressModels(asset, network)
            .toImmutableList(),
        showMemoDisclaimer = showMemoDisclaimer,
        notifications = persistentListOf(defaultAssetNotificationConfig(asset, network)).addAll(0, customNotifications),
        onCopyClick = onCopyClick,
        onShareClick = onShareClick,
    )

    companion object {
        private fun defaultAssetNotificationConfig(asset: Asset, network: Network): NotificationConfig =
            NotificationConfig(
                title = resourceReference(
                    R.string.receive_bottom_sheet_warning_title,
                    wrappedList(asset.displaySymbol, network.name),
                ),
                subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
                iconResId = R.drawable.ic_alert_circle_24,
                iconTint = NotificationConfig.IconTint.Accent,
            )
    }

    @Immutable
    sealed class Asset {
        abstract val displaySymbol: TextReference

        data class Currency(val name: String, val symbol: String) : Asset() {
            override val displaySymbol = stringReference(symbol)
        }

        data object NFT : Asset() {
            override val displaySymbol = resourceReference(R.string.common_nft)
        }
    }
}