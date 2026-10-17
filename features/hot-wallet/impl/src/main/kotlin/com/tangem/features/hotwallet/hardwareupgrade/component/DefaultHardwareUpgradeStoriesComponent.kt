package com.tangem.features.hotwallet.hardwareupgrade.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.features.hotwallet.HardwareUpgradeStoriesComponent
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.StoryV2Type
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultHardwareUpgradeStoriesComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: HardwareUpgradeStoriesComponent.Params,
    storiesV2ComponentFactory: StoriesV2Component.Factory,
) : HardwareUpgradeStoriesComponent, AppComponentContext by context {

    private val player: StoriesV2Component = storiesV2ComponentFactory.create(
        context = child(key = PLAYER_KEY),
        params = StoriesV2Component.Params(
            type = StoryV2Type.HARDWARE_UPGRADE,
            source = params.source.toStoryV2Source(),
            // Every button leads to the Hardware wallet screen, where the story ends anyway.
            onAction = StoriesV2PendingAction::end,
            onResult = { openHardwareWallet() },
            // Without the story the upgrade goes on to the Hardware wallet screen rather than waiting on a retry.
            shouldSkipOnDeliveryFailure = true,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        player.Content(modifier)
    }

    /**
     * Waits until the story is on top: `replaceCurrent` replaces the top screen, which may be one opened over the story

     */
    private fun openHardwareWallet() {
        lifecycle.doOnResume(isOneTime = true) {
            componentScope.launch(dispatchers.main) {
                router.replaceCurrent(AppRoute.WalletHardwareBackup(userWalletId = params.userWalletId))
            }
        }
    }

    private fun AppRoute.HardwareUpgradeStories.Source.toStoryV2Source(): StoryV2Source = when (this) {
        AppRoute.HardwareUpgradeStories.Source.MAIN -> StoryV2Source.MAIN
        AppRoute.HardwareUpgradeStories.Source.WALLET_SETTINGS -> StoryV2Source.WALLET_SETTINGS
    }

    @AssistedFactory
    interface Factory : HardwareUpgradeStoriesComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: HardwareUpgradeStoriesComponent.Params,
        ): DefaultHardwareUpgradeStoriesComponent
    }

    private companion object {
        const val PLAYER_KEY = "hardwareUpgradeStory"
    }
}