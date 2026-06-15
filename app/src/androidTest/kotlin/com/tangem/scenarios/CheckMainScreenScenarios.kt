package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onMainScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkSingleCurrencyMainScreen(cardTitle: String) {
    step("Assert card title equal '$cardTitle'") {
        onMainScreen { walletNameText.assertTextEquals(cardTitle) }
    }
    step("Assert 'Add funds' button is displayed") {
        onMainScreen { addFundsButton.assertIsDisplayed() }
    }
    step("Assert 'Sell' button is displayed") {
        onMainScreen { sellButton.assertIsDisplayed() }
    }
    step("Assert 'Swap' button is not displayed") {
        onMainScreen { swapButton.assertIsNotDisplayed() }
    }
    step("Swipe up") {
        swipeVertical(SwipeDirection.UP)
    }
    step("Assert 'Add & Manage' button is not displayed") {
        onMainScreen { addAndManageButtonWithoutLazySearch.assertIsNotDisplayed() }
    }
}

fun BaseTestCase.checkMultiCurrencyMainScreen(
    cardTitle: String,
) {
    step("Assert card title equal '$cardTitle'") {
        onMainScreen { walletNameText.assertTextEquals(cardTitle) }
    }
    step("Assert 'Add funds' button is displayed") {
        onMainScreen { addFundsButton.assertIsDisplayed() }
    }
    step("Assert 'Swap' button is displayed") {
        onMainScreen { swapButton.assertIsDisplayed() }
    }
    step("Assert 'Sell' button is displayed") {
        onMainScreen { sellButton.assertIsDisplayed() }
    }
    step("Assert 'Send' button is not displayed") {
        onMainScreen { sendButton.assertIsNotDisplayed() }
    }
    step("Assert 'Receive' button is not displayed") {
        onMainScreen { receiveButton.assertIsNotDisplayed() }
    }
    step("Assert 'Add & Manage' button is displayed") {
        onMainScreen { addAndManageButton().assertIsDisplayed() }
    }
}

fun BaseTestCase.assertActionButtonsForMultiCurrencyWallet(isEnabled: Boolean = true) {
    if (isEnabled) {
        step("Assert 'Add funds' button is enabled") {
            onMainScreen { addFundsButton.assertIsEnabled() }
        }
        step("Assert 'Swap' button is enabled") {
            onMainScreen { swapButton.assertIsEnabled() }
        }
        step("Assert 'Sell' button is enabled") {
            onMainScreen { sellButton.assertIsEnabled() }
        }
    } else {
        step("Assert 'Add funds' button is not enabled") {
            onMainScreen { addFundsButton.assertIsNotEnabled() }
        }
        step("Assert 'Swap' button is not enabled") {
            onMainScreen { swapButton.assertIsNotEnabled() }
        }
        step("Assert 'Sell' button is not enabled") {
            onMainScreen { sellButton.assertIsNotEnabled() }
        }
    }
}