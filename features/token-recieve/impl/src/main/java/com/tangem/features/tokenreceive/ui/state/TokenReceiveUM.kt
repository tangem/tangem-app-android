package com.tangem.features.tokenreceive.ui.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

internal data class TokenReceiveUM(
    val network: String,
    val iconState: CurrencyIconState,
    val addresses: ImmutableMap<Int, ReceiveAddress>,
    val isEnsResultLoading: Boolean,
    val notificationConfigs: ImmutableList<NotificationUM>,
)