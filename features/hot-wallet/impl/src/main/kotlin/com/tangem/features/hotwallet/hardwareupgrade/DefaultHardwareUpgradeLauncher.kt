package com.tangem.features.hotwallet.hardwareupgrade

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.pushIfAbsent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import com.tangem.features.storiesv2.GetHardwareUpgradeStoriesVariantUseCase
import com.tangem.features.storiesv2.HardwareUpgradeStoriesVariant
import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoryV2Type
import javax.inject.Inject

internal class DefaultHardwareUpgradeLauncher @Inject constructor(
    private val appRouter: AppRouter,
    private val getHardwareUpgradeStoriesVariantUseCase: GetHardwareUpgradeStoriesVariantUseCase,
    private val storiesV2Prefetcher: StoriesV2Prefetcher,
) : HardwareUpgradeLauncher {

    override suspend fun launch(userWalletId: UserWalletId, source: AppRoute.HardwareUpgradeStories.Source) {
        val route = when (getHardwareUpgradeStoriesVariantUseCase()) {
            HardwareUpgradeStoriesVariant.TREATMENT -> AppRoute.HardwareUpgradeStories(userWalletId, source)
            HardwareUpgradeStoriesVariant.CONTROL -> AppRoute.WalletHardwareBackup(userWalletId)
        }
        appRouter.pushIfAbsent(route)
    }

    override fun prefetch() {
        if (getHardwareUpgradeStoriesVariantUseCase.peek() == HardwareUpgradeStoriesVariant.TREATMENT) {
            storiesV2Prefetcher.prefetch(StoryV2Type.HARDWARE_UPGRADE)
        }
    }
}