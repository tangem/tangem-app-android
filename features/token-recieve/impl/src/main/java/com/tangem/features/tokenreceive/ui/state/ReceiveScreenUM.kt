package com.tangem.features.tokenreceive.ui.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tokenreceive.entity.ReceiveExtrasHint
import kotlinx.collections.immutable.ImmutableList

internal data class ReceiveScreenUM(
    val title: TextReference,
    val subtitle: TextReference,
    val iconState: CurrencyIconState,
    val address: String,
    val ensName: String?,
    val extrasHint: ReceiveExtrasHint,
    val networkNotice: TextReference,
    val warnings: ImmutableList<NotificationUM>,
    val onCopyAddressClick: () -> Unit,
    val onCopyEnsClick: () -> Unit,
)