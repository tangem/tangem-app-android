package com.tangem.features.tokenreceive.ui.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import kotlinx.collections.immutable.ImmutableList

internal data class ReceiveAssetsUM(
    val showMemoDisclaimer: Boolean,
    val addresses: ImmutableList<ReceiveAddress>,
    val onOpenQrCodeClick: (address: String) -> Unit,
    val onCopyClick: (address: ReceiveAddress) -> Unit,
    val isEnsResultLoading: Boolean,
    val notificationConfigs: ImmutableList<NotificationUM>,
)