package com.tangem.feature.walletsettings.ui.state

import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import kotlinx.collections.immutable.ImmutableList

internal data class NetworksAvailableForNotificationsUM(
    val networks: ImmutableList<NotificationsEligibleNetwork>,
    val isLoading: Boolean,
)