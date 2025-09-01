package com.tangem.features.tokenreceive.ui.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

internal data class ReceiveAssetsUM(
    val showMemoDisclaimer: Boolean,
    val addresses: ImmutableMap<Int, ReceiveAddress>,
    val onOpenQrCodeClick: (id: Int) -> Unit,
    val onCopyClick: (id: Int) -> Unit,
    val isEnsResultLoading: Boolean,
    val notificationConfigs: ImmutableList<NotificationUM>,
)