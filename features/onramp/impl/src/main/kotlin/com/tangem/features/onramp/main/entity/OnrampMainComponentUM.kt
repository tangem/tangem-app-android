package com.tangem.features.onramp.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface OnrampMainComponentUM {

    val topBarConfig: OnrampMainTopBarUM
    val errorNotification: NotificationUM?

    val buyNotSupportedMessage: TangemMessageUM?

    data class InitialLoading(
        override val topBarConfig: OnrampMainTopBarUM,
        override val errorNotification: NotificationUM?,
        override val buyNotSupportedMessage: TangemMessageUM? = null,
    ) : OnrampMainComponentUM

    data class Content(
        override val topBarConfig: OnrampMainTopBarUM,
        override val errorNotification: NotificationUM?,
        val amountBlockState: OnrampAmountBlockUM,
        val offersBlockState: OnrampOffersBlockUM,
        val onrampAmountButtonUMState: OnrampAmountButtonUMState,
        override val buyNotSupportedMessage: TangemMessageUM? = null,
    ) : OnrampMainComponentUM
}

internal data class OnrampMainTopBarUM(
    val title: TextReference,
    val startButtonUM: TopAppBarButtonUM,
    val endButtonUM: TopAppBarButtonUM,
)