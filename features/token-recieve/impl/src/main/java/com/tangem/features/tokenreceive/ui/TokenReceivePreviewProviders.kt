package com.tangem.features.tokenreceive.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.QrCodeUM
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import com.tangem.features.tokenreceive.ui.state.WarningUM
import kotlinx.collections.immutable.persistentListOf

internal class TokenReceiveAssetsContentProvider : PreviewParameterProvider<ReceiveAssetsUM> {
    val address = ReceiveAddress(
        value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        type = ReceiveAddress.Type.Primary.Default(
            displayName = stringReference("Ethereum address"),
        ),
    )
    private val dynamicAddress = ReceiveAddress(
        value = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
        type = ReceiveAddress.Type.Primary.Dynamic(
            displayName = stringReference("Ethereum address"),
        ),
    )
    private val tokenIconState = CurrencyIconState.TokenIcon(
        url = null,
        topBadgeIconResId = null,
        fallbackTint = TangemColorPalette.Black,
        fallbackBackground = TangemColorPalette.Meadow,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )
    private val config = ReceiveAssetsUM(
        notificationConfigs =
        persistentListOf(
            NotificationUM.Warning(
                title = stringReference("Send only XLM on the Ethereum network"),
                subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
            ),
        ),
        addresses = persistentListOf(
            address,
            address,
            address.copy(type = ReceiveAddress.Type.Ens, value = "papasha.eth"),
        ),
        showMemoDisclaimer = false,
        onCopyClick = {},
        onOpenQrCodeClick = {},
        isEnsResultLoading = true,
        network = "USDT",
        currencyIconState = CurrencyIconState.Locked,
        onShareClick = {},
    )
    private val yieldSupplyIsActiveConfig = config.copy(
        notificationConfigs = persistentListOf(
            NotificationUM.Warning.YieldSupplyIsActive(tokenName = "USDT"),
        ),
        isEnsResultLoading = false,
        currencyIconState = tokenIconState,
    )
    private val dynamicAddressConfig = config.copy(
        addresses = persistentListOf(
            dynamicAddress,
            address,
            address.copy(type = ReceiveAddress.Type.Ens, value = "papasha.eth"),
        ),
        isEnsResultLoading = false,
        notificationConfigs = persistentListOf(),
        currencyIconState = tokenIconState,
    )

    override val values: Sequence<ReceiveAssetsUM>
        get() = sequenceOf(config, yieldSupplyIsActiveConfig, dynamicAddressConfig)
}

internal class TokenReceiveQrCodeContentPreviewProvider : PreviewParameterProvider<QrCodeUM> {
    private val config = QrCodeUM(
        network = "Ethereum",
        addressName = stringReference("Ethereum"),
        addressValue = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        onCopyClick = {},
        onShareClick = {},
    )

    override val values: Sequence<QrCodeUM>
        get() = sequenceOf(config)
}

internal class TokenReceiveWarningContentProvider : PreviewParameterProvider<WarningUM> {
    val iconState = CurrencyIconState.TokenIcon(
        url = null,
        topBadgeIconResId = null,
        fallbackTint = TangemColorPalette.Black,
        fallbackBackground = TangemColorPalette.Meadow,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )

    override val values: Sequence<WarningUM>
        get() = sequenceOf(
            WarningUM(
                iconState = iconState,
                onWarningAcknowledged = {},
                network = "Ethereum",
            ),
        )
}