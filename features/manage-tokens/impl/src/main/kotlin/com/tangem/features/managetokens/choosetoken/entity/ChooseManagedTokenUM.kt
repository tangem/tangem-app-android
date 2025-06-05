package com.tangem.features.managetokens.choosetoken.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM

internal data class ChooseManagedTokenUM(
    val notificationUM: NotificationUM?,
    val readContent: ManageTokensUM.ReadContent,
)