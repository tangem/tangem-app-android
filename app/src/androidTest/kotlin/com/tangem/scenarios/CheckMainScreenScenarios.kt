package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.swipeVertical
import com.tangem.screens.onMainScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkSingleCurrencyMainScreen(
    cardBlockchain: String,
    cardTitle: String,
    withTransactions: Boolean = false,
    withWalletImage: Boolean = true
) {
    step("Assert card title equal '$cardTitle'") {
        onMainScreen { walletNameText.assertTextEquals(cardTitle) }
    }
    if (withWalletImage) {
        step("Assert card image is displayed") { //TODO: create assertion method for checking images
            onMainScreen { walletImage.assertIsDisplayed() }
        }
    } else {
        step("Assert card image is not displayed") {
            onMainScreen { walletImage.assertIsNotDisplayed() }
        }
    }
    step("Assert 'Receive' button is displayed") {
        onMainScreen { receiveButton.assertIsDisplayed() }
    }
    step("Assert 'Buy' button is displayed") {
        onMainScreen { buyButton.assertIsDisplayed() }
    }
    step("Assert 'Send' button is displayed") {
        onMainScreen { sendButton.assertIsDisplayed() }
    }
    step("Assert 'Sell' button is displayed") {
        onMainScreen { sellButton.assertIsDisplayed() }
    }
    step("Assert 'Swap' button is not displayed") {
        onMainScreen { swapButton.assertIsNotDisplayed() }
    }
    step("Assert 'Market Price' on single card main screen is displayed") {
        onMainScreen { marketPriceBlock().assertIsDisplayed() }
    }
    step("Assert 'Market Price' title equals $cardBlockchain Market Price") {
        onMainScreen { marketPriceText.assertTextContains("$cardBlockchain Market Price") }
    }
    step("Swipe up") {
        swipeVertical(SwipeDirection.UP)
    }
    if (withTransactions) {
        step("Assert 'Transactions' block is displayed") {
            onMainScreen { transactionsExplorerText.assertIsDisplayed() }
        }
        step("Assert 'Transactions' title is displayed") {
            onMainScreen { transactionsTitle.assertIsDisplayed() }
        }
        step("Assert 'Explorer' icon is displayed") {
            onMainScreen { transactionsExplorerIcon.assertIsDisplayed() }
        }
    } else {
        step("Assert empty 'Transactions' block is displayed") {
            onMainScreen { emptyTransactionBlock.assertIsDisplayed() }
        }
        step("Assert empty 'Transactions' block icon is displayed") {
            onMainScreen { emptyTransactionBlockIcon.assertIsDisplayed() }
        }
        step("Assert empty 'Transactions' block text is displayed") {
            onMainScreen { emptyTransactionBlockText.assertIsDisplayed() }
        }
        step("Assert empty 'Transactions' block 'Explore' button is displayed") {
            onMainScreen { emptyTransactionBlockExploreButton.assertIsDisplayed() }
        }
    }
    step("Assert 'Organize tokens' button is not displayed") {
        onMainScreen { organizeTokensButtonWithoutLazySearch.assertIsNotDisplayed() }
    }
}

fun BaseTestCase.checkMultiCurrencyMainScreen(
    devicesCount: String,
    cardTitle: String,
    withWalletImage: Boolean = true
) {
    step("Assert card title equal '$cardTitle'") {
        onMainScreen { walletNameText.assertTextEquals(cardTitle) }
    }
    if (withWalletImage) {
        step("Assert card image is displayed") {
            onMainScreen { walletImage.assertIsDisplayed() }
        }
    } else {
        step("Assert card image is not displayed") {
            onMainScreen { walletImage.assertIsNotDisplayed() }
        }
    }
    step("Assert devices count equal to '$devicesCount'") {
        onMainScreen { walletDevicesCount.assertTextContains(devicesCount) }
    }
    step("Assert 'Buy' button is displayed") {
        onMainScreen { buyButton.assertIsDisplayed() }
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
    step("Assert 'Organize tokens' button is displayed") {
        onMainScreen { organizeTokensButton().assertIsDisplayed() }
    }
}