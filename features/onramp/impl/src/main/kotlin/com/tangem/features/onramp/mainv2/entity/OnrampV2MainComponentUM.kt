package com.tangem.features.onramp.mainv2.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface OnrampV2MainComponentUM {

    val topBarConfig: OnrampV2MainTopBarUM
    val errorNotification: NotificationUM?

    data class InitialLoading(
        override val topBarConfig: OnrampV2MainTopBarUM,
        override val errorNotification: NotificationUM?,
    ) : OnrampV2MainComponentUM

    data class Content(
        override val topBarConfig: OnrampV2MainTopBarUM,
        override val errorNotification: NotificationUM?,
        val amountBlockState: OnrampNewAmountBlockUM,
        val offersBlockState: OnrampOffersBlockUM,
        val onrampAmountButtonUMState: OnrampV2AmountButtonUMState,
    ) : OnrampV2MainComponentUM
}

internal data class OnrampV2MainTopBarUM(
    val title: TextReference,
    val startButtonUM: TopAppBarButtonUM,
    val endButtonUM: TopAppBarButtonUM,
)