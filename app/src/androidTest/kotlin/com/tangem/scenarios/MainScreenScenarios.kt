package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOrganizeTokensScreen
import io.qameta.allure.kotlin.Allure.step
import org.junit.Assert.assertEquals

fun BaseTestCase.openOrganizeTokensScreen() {
    step("Swipe to 'Add & Manage' button") {
        swipeVertical(SwipeDirection.UP)
        swipeVertical(SwipeDirection.UP)
    }
    step("Click on 'Add & Manage' button") {
        onMainScreen { addAndManageButton().clickWithAssertion() }
    }
    step("Click on 'Organize tokens' button in bottom sheet") {
        onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
    }
}

fun BaseTestCase.getMainScreenTokensOrder(): List<String> {
    var tokens: List<String> = emptyList()
    step("Read displayed token titles from 'Main' screen") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onMainScreen { tokens = getDisplayedTokenTitles() }
            require(tokens.isNotEmpty()) { "No token titles found on the main screen" }
        }
    }
    return tokens
}

fun BaseTestCase.assertOrganizeTokensMatch(expectedTokens: List<String>) {
    step("Open 'Organize tokens' bottom-sheet") {
        onMainScreen { clickDisplayedAddAndManageButton() }
        onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
    }
    step("Assert 'Organize tokens' list matches the main screen order") {
        onOrganizeTokensScreen {
            assertEquals(expectedTokens, getDisplayedTokenTitles())
        }
    }
    step("Return to 'Main' screen") {
        onOrganizeTokensScreen { cancelButton.clickWithAssertion() }
    }
}