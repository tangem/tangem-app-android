package com.tangem.features.swap.v2.impl.notifications

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.notifications.model.SwapNotificationsModel
import com.tangem.features.swap.v2.impl.notifications.ui.swapNotifications
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class SwapNotificationsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext {
    private val model: SwapNotificationsModel = getOrCreateModel(params)

    val state: StateFlow<ImmutableList<NotificationUM>> = model.uiState

    fun LazyListScope.content(
        state: ImmutableList<NotificationUM>,
        modifier: Modifier = Modifier,
        hasPaddingAbove: Boolean = false,
        isClickDisabled: Boolean = false,
    ) {
        swapNotifications(
            notifications = state,
            modifier = modifier,
            hasPaddingAbove = hasPaddingAbove,
            isClickDisabled = isClickDisabled,
        )
    }

    data class Params(
        val swapNotificationData: SwapNotificationData,
    ) {
        data class SwapNotificationData(
            val expressError: ExpressError?,
            val fromCryptoCurrency: CryptoCurrency?,
        )
    }
}