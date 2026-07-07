package com.tangem.features.send.subcomponents.notifications

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.api.SendNotificationsComponent
import com.tangem.features.send.api.SendNotificationsComponent.Params
import com.tangem.features.send.subcomponents.notifications
import com.tangem.features.send.subcomponents.notifications.model.NotificationsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class DefaultSendNotificationsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
) : SendNotificationsComponent, AppComponentContext by appComponentContext {

    private val model: NotificationsModel = getOrCreateModel(params)

    override val state: StateFlow<ImmutableList<NotificationUM>> = model.uiState

    override fun LazyListScope.content(
        state: ImmutableList<NotificationUM>,
        modifier: Modifier,
        hasPaddingAbove: Boolean,
        isClickDisabled: Boolean,
    ) {
        notifications(
            notifications = state,
            modifier = modifier,
            hasPaddingAbove = hasPaddingAbove,
            isClickDisabled = isClickDisabled,
        )
    }

    @AssistedFactory
    interface Factory : SendNotificationsComponent.Factory {
        override fun create(context: AppComponentContext, params: Params): DefaultSendNotificationsComponent
    }
}