package com.tangem.tests.main

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class HideTokenTest : BaseTestCase() {

    @AllureId("3638")
    @DisplayName("Main: hide token by long tap")
    @Test
    fun hideTokenByLongTapTest() {
        val tokenTitle = "Polygon"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Click on 'Hide token' button") {
                onTokenActionsBottomSheet { hideTokenButton.performClick() }
            }
            step("Click 'Hide' button in dialog") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    okButton.clickWithAssertion()
                }
            }
            step("Assert token: '$tokenTitle' is not displayed") {
                onMainScreen { assertTokenDoesNotExist(tokenTitle) }
            }
        }
    }

    @AllureId("3627")
    @DisplayName("Main: hide token via Manage tokens")
    @Test
    fun hideTokenViaManageTokensTest() {
        val tokenTitle = "Tether"
        val networkTitle = "ETHEREUM"
        val scenarioState = "USDT"
        val accountName = "Main account"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                waitForIdle()
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.performClick() }
            }
            step("Click on account: '$accountName'") {
                onWalletSettingsScreen { accountItem(accountName).performClick() }
            }
            step("Click on 'Manage tokens' button") {
                onAccountDetails { manageTokensButton.performClick() }
            }
            step("Click on token: '$tokenTitle'") {
                onManageTokensScreen { tokenItem(tokenTitle).performClick() }
            }
            step("Assert switch is on") {
                onManageTokensScreen { networkSwitch(networkTitle).assertIsOn() }
            }
            step("Click on '$networkTitle' switch") {
                onManageTokensScreen { networkSwitch(networkTitle).performClick() }
            }
            step("Click 'Hide' button in dialog") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    hideButton.clickWithAssertion()
                }
            }
            step("Assert switch is off") {
                onManageTokensScreen { networkSwitch(networkTitle).assertIsOff() }
            }
            step("Click on 'Save' button") {
                onManageTokensScreen { saveButton.performClick() }
            }
            step("Click on 'Account details' screen 'Back' button") {
                waitForIdle()
                onAccountDetails { topAppBarBackButton.performClick() }
            }
            step("Click on 'Wallet settings' screen 'Back' button") {
                waitForIdle()
                onWalletSettingsScreen { topAppBarBackButton.performClick() }
            }
            step("Click on 'Details' screen 'Back' button") {
                waitForIdle()
                onDetailsScreen { topAppBarBackButton.performClick() }
            }
            step("Assert token: '$tokenTitle' is not displayed") {
                onMainScreen { assertTokenDoesNotExist(tokenTitle) }
            }
        }
    }

    @AllureId("3626")
    @DisplayName("Main: hide main coin via manage tokens")
    @Test
    fun hideMainCoinViaManageTokensTest() {
        val tokenTitle = "POL (ex-MATIC)"
        val networkTitle = "POLYGON"
        val polygonTitle = "Polygon"
        val accountName = "Main account"

        setupHooks().run {

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open wallet details") {
                waitForIdle()
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.performClick() }
            }
            step("Click on account: '$accountName'") {
                onWalletSettingsScreen { accountItem(accountName).performClick() }
            }
            step("Click on 'Manage tokens' button") {
                onAccountDetails { manageTokensButton.performClick() }
            }
            step("Click on token: '$tokenTitle'") {
                onManageTokensScreen { tokenItem(tokenTitle).performClick() }
            }
            step("Assert switch is on") {
                onManageTokensScreen { networkSwitch(networkTitle).assertIsOn() }
            }
            step("Click on '$networkTitle' switch") {
                onManageTokensScreen { networkSwitch(networkTitle).performClick() }
            }
            step("Click 'Hide' button in dialog") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    hideButton.clickWithAssertion()
                }
            }
            step("Assert switch is off") {
                onManageTokensScreen { networkSwitch(networkTitle).assertIsOff() }
            }
            step("Click on 'Save' button") {
                onManageTokensScreen { saveButton.performClick() }
            }
            step("Click on 'Account details' screen 'Back' button") {
                waitForIdle()
                onAccountDetails { topAppBarBackButton.performClick() }
            }
            step("Click on 'Wallet settings' screen 'Back' button") {
                waitForIdle()
                onWalletSettingsScreen { topAppBarBackButton.performClick() }
            }
            step("Click on 'Details' screen 'Back' button") {
                waitForIdle()
                onDetailsScreen { topAppBarBackButton.performClick() }
            }
            step("Assert token: '$polygonTitle' is not displayed") {
                onMainScreen { assertTokenDoesNotExist(polygonTitle) }
            }
        }
    }

    @AllureId("3610")
    @DisplayName("Main: check 'Unable to hide token' warning")
    @Test
    fun checkUnableToHideTokenWarningTest() {
        val tokenTitle = "Ethereum"
        val tokenSymbol = "ETH"
        val scenarioState = "USDT"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Click on 'Hide token' button") {
                onTokenActionsBottomSheet { hideTokenButton.performClick() }
            }
            step("Assert 'Unable to hide $tokenTitle' alert title is displayed") {
                onUnableToHideDialog {
                    unableToHideTokenTitle(tokenName = tokenTitle).assertIsDisplayed()
                }
            }
            step("Assert 'Unable to hide $tokenTitle' alert message is displayed") {
                onUnableToHideDialog {
                    unableToHideTokenMessage(
                        tokenName = tokenTitle,
                        tokenSymbol = tokenSymbol,
                        networkName = tokenTitle
                    ).assertIsDisplayed()
                }
            }
            step("Click on 'Ok' button") {
                onUnableToHideDialog {
                    okButton.performClick()
                }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Assert token: '$tokenTitle' is displayed") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).assertIsDisplayed() }
            }
            step("Click on token: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click 'More' button") {
                onTokenDetailsTopBar { moreButton.clickWithAssertion() }
            }
            step("Click 'Hide token' button") {
                onPopUpMenu {
                    popUpContainer.assertIsDisplayed()
                    hideTokenButton.clickWithAssertion()
                }
            }
            step("Assert 'Unable to hide $tokenSymbol' alert title is displayed") {
                onUnableToHideDialog {
                    unableToHideTokenTitle(tokenName = tokenSymbol).assertIsDisplayed()
                }
            }
            step("Assert 'Unable to hide $tokenTitle' alert message is displayed") {
                onUnableToHideDialog {
                    unableToHideTokenMessage(
                        tokenName = tokenTitle,
                        tokenSymbol = tokenSymbol,
                        networkName = tokenTitle
                    ).assertIsDisplayed()
                }
            }
            step("Click on 'Ok' button") {
                onUnableToHideDialog {
                    okButton.performClick()
                }
            }
            step("Click 'Back' button") {
                onTokenDetailsTopBar { backButton.clickWithAssertion() }
            }
            step("Assert token: '$tokenTitle' is displayed") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).assertIsDisplayed() }
            }
        }
    }

}