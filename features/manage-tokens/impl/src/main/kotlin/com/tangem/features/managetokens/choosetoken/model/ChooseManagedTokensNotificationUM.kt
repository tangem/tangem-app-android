package com.tangem.features.managetokens.choosetoken.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.managetokens.impl.R

internal object ChooseManagedTokensNotificationUM {

    data class SendViaSwap(
        val onCloseClick: () -> Unit,
    ) : NotificationUM.Info(
        title = resourceReference(R.string.send_with_swap_title),
        subtitle = resourceReference(R.string.send_with_swap_notification_text),
        iconResId = R.drawable.ic_exchange_horizontal_24,
        onCloseClick = onCloseClick,
    )
}