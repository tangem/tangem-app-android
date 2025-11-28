package com.tangem.feature.walletsettings.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class WalletSettingsUM(
    val popBack: () -> Unit,
    val items: PersistentList<WalletSettingsItemUM>,
    val hasRequestPushNotificationsPermission: Boolean = false,
    val onPushNotificationPermissionGranted: (Boolean) -> Unit,
    val accountReorderUM: AccountReorderUM,
    val isWalletBackedUp: Boolean = true,
    val isWalletUpgradeDismissed: Boolean = false,
)

internal data class AccountReorderUM(
    val isDragEnabled: Boolean,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    val onDragStopped: () -> Unit,
)