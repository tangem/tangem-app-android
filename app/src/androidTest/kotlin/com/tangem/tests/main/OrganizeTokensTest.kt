package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.addNewCardWalletWithoutSync
import com.tangem.scenarios.assertOrganizeTokensMatch
import com.tangem.scenarios.getMainScreenTokensOrder
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.switchToPreviousWallet
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOrganizeTokensScreen
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class OrganizeTokensTest : BaseTestCase() {

    @AllureId("71")
    @DisplayName("Organize tokens: Correct tokens list displaying for current wallet")
    @Test
    fun organizeTokensCorrectTokensListDisplaying() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Add a second card wallet") {
                addNewCardWalletWithoutSync(Wallet2MockContent)
            }
            step("Switch to first wallet Main screen") {
                switchToPreviousWallet()
            }

            val firstWalletTokens = getMainScreenTokensOrder()
            assertOrganizeTokensMatch(firstWalletTokens)

            step("Switch to second wallet Main screen") {
                onMainScreen { swipeToAdjacentWallet(toPrevious = false) }
            }

            val secondWalletTokens = getMainScreenTokensOrder()
            assertOrganizeTokensMatch(secondWalletTokens)
        }
    }

    @AllureId("2753")
    @DisplayName("Organize tokens: Tokens order changing")
    @Test
    fun organizeTokensOrderChanging() {
        setupHooks().run{
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Organize tokens' bottom-sheet") {
                onMainScreen { clickDisplayedAddAndManageButton() }
                onAddAndManageBottomSheet { organizeTokensButton.clickWithAssertion() }
            }
            step("Drag the 3rd token onto the 2nd position and assert the new order") {
                onOrganizeTokensScreen {
                    val sourceIndex = 2
                    val destinationIndex = 1
                    val before = getDisplayedTokenTitles()
                    require(before.size > sourceIndex && before.size > destinationIndex) {
                        "Expected at least ${maxOf(sourceIndex, destinationIndex) + 1} tokens to reorder, but got ${before.size}: $before"
                    }
                    dragToken(source = before[sourceIndex], destination = before[destinationIndex])
                    val expected = before.toMutableList().apply {
                        add(destinationIndex, removeAt(sourceIndex))
                    }
                    assertEquals(expected, getDisplayedTokenTitles())
                }
            }
        }
    }
}