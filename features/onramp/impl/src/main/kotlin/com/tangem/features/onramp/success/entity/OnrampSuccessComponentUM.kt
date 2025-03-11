package com.tangem.features.onramp.success.entity

import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference

sealed class OnrampSuccessComponentUM {

    data object Loading : OnrampSuccessComponentUM()

    data class Content(
        val txId: String,
        val timestamp: Long,
        val currencyImageUrl: String?,
        val fromAmount: TextReference,
        val toAmount: TextReference,
        val statusBlock: ExpressStatusUM,
        val providerName: TextReference,
        val providerImageUrl: String,
        val notification: NotificationUM?,
    ) : OnrampSuccessComponentUM()
}