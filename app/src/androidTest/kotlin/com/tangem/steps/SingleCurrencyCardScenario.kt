package com.tangem.steps

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.assertElementDoesNotExist
import com.tangem.screens.onMainScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkSingleCurrencyMainScreen(cardBlockchain: String, cardTitle: String) {
    step("Assert card title equal '$cardTitle'") {
        onMainScreen { walletNameText.assertTextEquals(cardTitle) }
    }
    step("Assert  'Transactions' block is displayed") {
        onMainScreen { transactionsExplorer().assertExists() }
    }
    step("Assert 'Transactions' title is displayer") {
        onMainScreen { transactionsTitle.assertIsDisplayed() }
    }
    step("Assert 'Explorer Icon' is displayed") {
        onMainScreen { transactionsExplorerIcon.assertIsDisplayed() }
    }
    step("Assert card image is displayed") { //TODO: Придумать нормальную проверку / реализовать скриншот-тестинг
        onMainScreen { walletImage.assertIsDisplayed() }
    }
    step("Assert 'Market Price' on single card main screen is displayed") {
        onMainScreen { marketPriceBlock.assertIsDisplayed() }
    }
    step("Assert 'Market Price' title equals $cardBlockchain Market Price") {
        onMainScreen { marketPriceText.assertTextContains("$cardBlockchain Market Price") }
    }
    step("Assert  'Organize tokens' button is not displayed") {
        onMainScreen {
            assertElementDoesNotExist({ organizeTokensButtonWithoutLazySearch() }, "Organize tokens button")
        }
    }
}