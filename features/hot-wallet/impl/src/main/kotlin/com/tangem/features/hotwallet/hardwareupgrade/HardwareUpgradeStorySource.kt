package com.tangem.features.hotwallet.hardwareupgrade

import com.tangem.common.routing.AppRoute
import com.tangem.features.storiesv2.StoryV2Source

internal fun AppRoute.WalletHardwareBackup.Source.toStoryV2Source(): StoryV2Source = when (this) {
    AppRoute.WalletHardwareBackup.Source.UPGRADE_BANNER,
    AppRoute.WalletHardwareBackup.Source.BACKUP,
    -> StoryV2Source.MAIN
    AppRoute.WalletHardwareBackup.Source.PERMANENT_BANNER -> StoryV2Source.PERMANENT_BANNER
    AppRoute.WalletHardwareBackup.Source.WALLET_SETTINGS -> StoryV2Source.WALLET_SETTINGS
}