package com.tangem.features.home.impl.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.stories.StoriesContainer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.StoriesScreenTestTags
import com.tangem.features.home.impl.ui.compose.content.FirstStoriesContent
import com.tangem.features.home.impl.ui.compose.content.StoriesCurrencies
import com.tangem.features.home.impl.ui.compose.content.StoriesRevolutionaryWallet
import com.tangem.features.home.impl.ui.compose.content.StoriesUltraSecureBackup
import com.tangem.features.home.impl.ui.compose.content.StoriesWalletForEveryone
import com.tangem.features.home.impl.ui.compose.content.StoriesWeb3
import com.tangem.features.home.impl.ui.compose.views.HomeButtonsV2
import com.tangem.features.home.impl.ui.state.HomeUM
import com.tangem.features.home.impl.ui.state.Stories

private const val BACKGROUND_COLOR = 0xFF010101L

/**
 * Home stories built on the shared [StoriesContainer].
 * The container provides the progress bar, tap/hold navigation and pause; this screen supplies the
 * per-story content, the Tangem logo and the persistent "Get Started" button.
 */
@Composable
internal fun HomeStoriesScreen(state: HomeUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(BACKGROUND_COLOR))
            .testTag(StoriesScreenTestTags.SCREEN_CONTAINER),
    ) {
        StoriesContainer(
            modifier = Modifier.fillMaxSize(),
            config = state.storiesConfig,
            isPauseStories = state.scanInProgress,
        ) { story, isPaused ->
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tangem_logo),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .padding(
                            start = TangemTheme.dimens.spacing16,
                            top = TangemTheme.dimens.spacing16,
                        )
                        .height(TangemTheme.dimens.size18)
                        .align(Alignment.Start),
                )
                when (story) {
                    Stories.TangemIntro -> FirstStoriesContent(isPaused = isPaused, duration = story.duration)
                    Stories.RevolutionaryWallet -> StoriesRevolutionaryWallet()
                    Stories.UltraSecureBackup -> StoriesUltraSecureBackup(
                        isPaused = isPaused,
                        stepDuration = story.duration,
                    )
                    Stories.Currencies -> StoriesCurrencies(isPaused, story.duration)
                    Stories.Web3 -> StoriesWeb3(isPaused, story.duration)
                    Stories.WalletForEveryone -> StoriesWalletForEveryone(story.duration)
                }
            }
        }
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = TangemTheme.dimens.spacing16)
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            HomeButtonsV2(
                modifier = Modifier.fillMaxWidth(),
                onGetStartedClick = state.onGetStartedClick,
            )
        }
    }
}