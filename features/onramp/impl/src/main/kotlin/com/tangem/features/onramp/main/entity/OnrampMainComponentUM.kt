package com.tangem.features.onramp.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface OnrampMainComponentUM {

    val topBarConfig: OnrampMainTopBarUM
    val errorNotification: NotificationUM?

    data class InitialLoading(
        override val topBarConfig: OnrampMainTopBarUM,
        override val errorNotification: NotificationUM?,
    ) : OnrampMainComponentUM

    data class Content(
        override val topBarConfig: OnrampMainTopBarUM,
        override val errorNotification: NotificationUM?,
        val amountBlockState: OnrampAmountBlockUM,
        val offersBlockState: OnrampOffersBlockUM,
        val onrampAmountButtonUMState: OnrampAmountButtonUMState,
    ) : OnrampMainComponentUM
}

internal data class OnrampMainTopBarUM(
    val title: TextReference,
    val startButtonUM: TopAppBarButtonUM,
    val endButtonUM: TopAppBarButtonUM,
)