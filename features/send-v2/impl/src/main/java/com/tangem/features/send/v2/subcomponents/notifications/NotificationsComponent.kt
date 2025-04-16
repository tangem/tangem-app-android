package com.tangem.features.send.v2.subcomponents.notifications

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.v2.subcomponents.notifications
import com.tangem.features.send.v2.subcomponents.notifications.model.NotificationData
import com.tangem.features.send.v2.subcomponents.notifications.model.NotificationsModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class NotificationsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext {

    private val model: NotificationsModel = getOrCreateModel(params)

    val state: StateFlow<ImmutableList<NotificationUM>> = model.uiState

    fun LazyListScope.content(
        state: ImmutableList<NotificationUM>,
        modifier: Modifier = Modifier,
        hasPaddingAbove: Boolean = false,
        isClickDisabled: Boolean = false,
    ) {
        notifications(
            notifications = state,
            modifier = modifier,
            hasPaddingAbove = hasPaddingAbove,
            isClickDisabled = isClickDisabled,
        )
    }

    data class Params(
        val analyticsCategoryName: String,
        val userWalletId: UserWalletId,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrency: AppCurrency,
        val notificationData: NotificationData,
    )
}