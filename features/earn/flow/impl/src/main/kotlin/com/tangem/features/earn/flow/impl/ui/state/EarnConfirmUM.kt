package com.tangem.features.earn.flow.impl.ui.state

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.ui.EarnRowUM
import kotlinx.collections.immutable.ImmutableList

/** [amountCrypto] and [amountFiat] show the amount that goes into the protocol, after the fee is deducted. */
internal data class EarnConfirmUM(
    val icons: EarnIconsUM,
    val amountFiat: TextReference,
    val amountCrypto: TextReference,
    val rows: ImmutableList<EarnRowUM>,
    val notice: TextReference?,
    val notifications: ImmutableList<NotificationUM>,
    val legal: TextReference?,
    val stepLabel: TextReference?,
    val primary: TangemButtonUM,
)