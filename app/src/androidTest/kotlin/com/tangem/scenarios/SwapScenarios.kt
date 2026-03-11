package com.tangem.scenarios

import androidx.compose.ui.test.click
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.*
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import com.tangem.common.ui.R as CommonUiR

private val firstStoryIndex = 0
private val firstStoryTitle = getResourceString(CommonUiR.string.swap_story_first_title)
private val firstStorySubtitle = getResourceString(CommonUiR.string.swap_story_first_subtitle)
private val secondStoryIndex = 1
private val secondStoryTitle = getResourceString(CommonUiR.string.swap_story_second_title)
private val secondStorySubtitle = getResourceString(CommonUiR.string.swap_story_second_subtitle)
private val thirdStoryIndex = 2
private val thirdStoryTitle = getResourceString(CommonUiR.string.swap_story_third_title)
private val thirdStorySubtitle = getResourceString(CommonUiR.string.swap_story_third_subtitle)
private val forthStoryIndex = 3
private val forthStoryTitle = getResourceString(CommonUiR.string.swap_story_forth_title)
private val forthStorySubtitle = getResourceString(CommonUiR.string.swap_story_forth_subtitle)
private val fifthStoryIndex = 4
private val fifthStoryTitle = getResourceString(CommonUiR.string.swap_story_fifth_title)
private val fifthStorySubtitle = getResourceString(CommonUiR.string.swap_story_fifth_subtitle)

fun BaseTestCase.openSwapScreen(
    from: SwapEntryPoint,
    storiesExist: Boolean = true,
) {
    when (from) {
        SwapEntryPoint.MainScreen -> step("Click on 'Swap' button on 'Main' screen") {
            onMainScreen { swapButton.performClick() }
        }

        SwapEntryPoint.TokenDetails -> step("Click on 'Swap' button on 'Token details' screen") {
            onTokenDetailsScreen { swapButton().performClick() }
        }

        SwapEntryPoint.MarketsTokenDetails -> step("Click on 'Swap' button on 'Markets' token details screen") {
            onMarketsTokenDetailsScreen { swapPortfolioQuickActionButton.performClick() }
        }

        SwapEntryPoint.TokenActionsBottomSheet -> step("Click on 'Swap' button on token actions bottom sheet") {
            onTokenActionsBottomSheet { swapButton.performClick() }
        }
    }

    if (storiesExist) {
        step("Close 'Stories' screen") {
            onSwapStoriesScreen { closeButton.clickWithAssertion() }
        }
    } else {
        step("Assert 'Stories' screen is not displayed") {
            onSwapStoriesScreen { container.assertDoesNotExist() }
        }
    }

    step("Assert 'Swap' screen title is displayed") {
        onSwapTokenScreen { title.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkStoriesContent(
    storyIndex: Int,
    storyTitle: String,
    storySubtitle: String,
    ) {
    step("Assert 'Close' button is displayed") {
        onSwapStoriesScreen { closeButton.assertIsDisplayed() }
    }
    step("Assert progress bar item №${storyIndex + 1} is displayed") {
        onSwapStoriesScreen { progressBarItem(storyIndex).assertIsDisplayed() }
    }
    step("Assert story title is $storyTitle") {
        onSwapStoriesScreen { title.assertTextContains(storyTitle) }
    }
    step("Assert story subtitle is $storySubtitle") {
        onSwapStoriesScreen { subtitle.assertTextContains(storySubtitle) }
    }
}

fun BaseTestCase.checkStoriesChanges() {
    step("Check title and subtitle for story №${firstStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = firstStoryIndex,
            storyTitle = firstStoryTitle,
            storySubtitle = firstStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${secondStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = secondStoryIndex,
            storyTitle = secondStoryTitle,
            storySubtitle = secondStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${thirdStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = thirdStoryIndex,
            storyTitle = thirdStoryTitle,
            storySubtitle = thirdStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${forthStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = forthStoryIndex,
            storyTitle = forthStoryTitle,
            storySubtitle = forthStorySubtitle
        )
    }
    step("Click on right side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerRight) } }
    }
    step("Check title and subtitle for story №${fifthStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = fifthStoryIndex,
            storyTitle = fifthStoryTitle,
            storySubtitle = fifthStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${forthStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = forthStoryIndex,
            storyTitle = forthStoryTitle,
            storySubtitle = forthStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${thirdStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = thirdStoryIndex,
            storyTitle = thirdStoryTitle,
            storySubtitle = thirdStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${secondStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = secondStoryIndex,
            storyTitle = secondStoryTitle,
            storySubtitle = secondStorySubtitle
        )
    }
    step("Click on left side") {
        onSwapStoriesScreen { container.performTouchInput { click(centerLeft) } }
    }
    step("Check title and subtitle for story №${firstStoryIndex + 1}") {
        checkStoriesContent(
            storyIndex = firstStoryIndex,
            storyTitle = firstStoryTitle,
            storySubtitle = firstStorySubtitle
        )
    }
}

sealed class SwapEntryPoint {
    object MainScreen : SwapEntryPoint()
    object TokenDetails : SwapEntryPoint()
    object MarketsTokenDetails : SwapEntryPoint()
    object TokenActionsBottomSheet : SwapEntryPoint()
}


