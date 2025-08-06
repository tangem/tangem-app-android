package com.tangem.tests

import androidx.compose.ui.test.onAllNodesWithText
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeUp
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOrganizeTokensScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class OrganizeTokensTest : BaseTestCase() {

    @AllureId("2755")
    @DisplayName("Organize tokens: group tokens")
    @Test
    fun groupTokensTest() {
        setupHooks().run {
            val tokenTitle = "Ethereum"
            val tokenNetwork = "Ethereum network"
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button" ) {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Swipe to 'Organize tokens' button") {
                swipeUp()
            }
            step("Click 'Organize tokens' button") {
                onMainScreen { organizeTokensButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' screen is opened") {
                onOrganizeTokensScreen {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).assertIsDisplayed()
                }
            }
            step("Click 'Group' button") {
                onOrganizeTokensScreen { groupButton.clickWithAssertion() }
            }
            step("Assert tokens were grouped on 'Organize tokens' screen") {
                onOrganizeTokensScreen { tokenNetworkGroupTitle(tokenNetwork).assertIsDisplayed() }
            }
            step("Click 'Apply' button") {
                onOrganizeTokensScreen { applyButton.clickWithAssertion() }
            }
            step("Assert tokens were grouped on 'Main screen'") {
                onMainScreen { tokenNetworkGroupTitle(tokenNetwork).assertIsDisplayed() }
            }
            step("Swipe to 'Organize tokens' button") {
                swipeUp()
            }
            step("Click 'Organize tokens' button") {
                onMainScreen { organizeTokensButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' screen is opened") {
                onOrganizeTokensScreen {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).assertIsDisplayed()
                }
            }
            step("Click 'Ungroup' button") {
                onOrganizeTokensScreen { ungroupButton.clickWithAssertion() }
            }
            step("Assert tokens were ungrouped on 'Organize tokens' screen") {
                onOrganizeTokensScreen { tokenNetworkGroupTitle(tokenNetwork).assertIsNotDisplayed() }
            }
            step("Click 'Apply' button") {
                onOrganizeTokensScreen { applyButton.clickWithAssertion() }
            }
            step("Assert tokens were ungrouped on 'Main screen'") {
                onMainScreen { tokenNetworkGroupTitle(tokenNetwork).assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("2752")
    @DisplayName("Organize tokens: check position of tokens")
    @Test
    fun checkPositionOfTokensTest() {
        setupHooks().run {
            val ethereumTitle = "Ethereum"
            val bitcoinTitle = "Bitcoin"
            val balance = TOTAL_BALANCE
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button" ) {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Check positions of tokens on 'Main Screen'") {
                onMainScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 0).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 1).assertIsDisplayed()
                }
            }
            step("Swipe to 'Organize tokens' button") {
                swipeUp()
            }
            step("Click 'Organize tokens' button") {
                onMainScreen { organizeTokensButton().clickWithAssertion() }
            }
            step("Check positions of tokens on 'Organize tokens' screen") {
                onOrganizeTokensScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 2).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("2753")
    @DisplayName("Organize tokens: check position of tokens")
    // toDo: on test build there is not ability to drag element
    // @Test
    fun checkCustomTokensOrderTest() {
        setupHooks().run {
            val ethereumTitle = "Ethereum"
            val bitcoinTitle = "Bitcoin"
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button" ) {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Check positions of tokens on 'Main Screen'") {
                onMainScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 0).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 1).assertIsDisplayed()
                }
            }
            step("Swipe to 'Organize tokens' button") {
                swipeUp()
            }
            step("Click 'Organize tokens' button") {
                onMainScreen { organizeTokensButton().clickWithAssertion() }
            }
            step("Drag $bitcoinTitle down on 'Organize tokens' screen") {
                composeTestRule.waitUntil(timeoutMillis = 100_000) {
                    composeTestRule.onAllNodesWithText("Data loaded").fetchSemanticsNodes().isNotEmpty()
                }
                onOrganizeTokensScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 2).assertIsDisplayed()
                }
            }
            step("Click 'Apply' button") {
                onOrganizeTokensScreen { applyButton.clickWithAssertion() }
            }
            step("Check positions of tokens on 'Main Screen'") {
                onMainScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 0).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("2754")
    @DisplayName("Organize tokens: sort by balance")
    @Test
    fun checkSortByBalanceTest() {
        setupHooks().run {
            val ethereumTitle = "Ethereum"
            val bitcoinTitle = "Bitcoin"
            val polygonTitle = "Polygon"
            val polExMaticTitle = "POL (ex-MATIC)"
            val balance = TOTAL_BALANCE
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button" ) {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = '$balance'") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Check positions of tokens on 'Main Screen'") {
                onMainScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 0).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(polygonTitle, 2).assertIsDisplayed()
                }
            }
            step("Swipe to 'Organize tokens' button") {
                swipeUp()
            }
            step("Click 'Organize tokens' button") {
                onMainScreen { organizeTokensButton().clickWithAssertion() }
            }
            step("Check positions of tokens on 'Organize tokens' screen") {
                onOrganizeTokensScreen {
                    tokenWithTitleAndPosition(bitcoinTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(ethereumTitle, 2).assertIsDisplayed()
                    tokenWithTitleAndPosition(polygonTitle, 3).assertIsDisplayed()
                    tokenWithTitleAndPosition(polExMaticTitle, 4).assertIsDisplayed()
                }
            }
            step("Click 'By Balance' button") {
                onOrganizeTokensScreen {
                    sortByBalanceButton.clickWithAssertion()
                }
            }
            step("Check positions of tokens by balance on 'Organize tokens' screen") {
                onOrganizeTokensScreen {
                    tokenWithTitleAndPosition(ethereumTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(polExMaticTitle, 2).assertIsDisplayed()
                    tokenWithTitleAndPosition(polygonTitle, 3).assertIsDisplayed()
                    tokenWithTitleAndPosition(bitcoinTitle, 4).assertIsDisplayed()
                }
            }
            step("Click 'Apply' button") {
                onOrganizeTokensScreen { applyButton.clickWithAssertion() }
            }
            step("Check positions of tokens by balance on 'Organize tokens' screen") {
                onMainScreen {
                    tokenWithTitleAndPosition(ethereumTitle, 0).assertIsDisplayed()
                    tokenWithTitleAndPosition(polExMaticTitle, 1).assertIsDisplayed()
                    tokenWithTitleAndPosition(polygonTitle, 2).assertIsDisplayed()
                    tokenWithTitleAndPosition(bitcoinTitle, 3).assertIsDisplayed()
                }
            }
        }
    }


}