package com.tangem.scenarios

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickAndWaitFor
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertUrlOpened
import com.tangem.common.utils.stubUrlOpening
import com.tangem.screens.onAddFundsBottomSheet
import com.tangem.screens.onBuyTokenDetailsScreen
import com.tangem.screens.onDialog
import com.tangem.screens.onMarketsTokenDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onOnrampSuccessScreen
import com.tangem.screens.onTokenActionsBottomSheet
import com.tangem.screens.onTokenDetailsScreen
import io.qameta.allure.kotlin.Allure.step

private const val CONTEXT_MENU_LONG_CLICK_MS = 1000L

fun BaseTestCase.openBuyScreenFromTokenDetails(tokenTitle: String, fiatAmount: String) {
    step("Click on token with name: '$tokenTitle'") {
        openTokenDetailsFromTokenList(tokenTitle)
    }
    step("Click on 'Add funds' button") {
        onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
    }
    step("Click on 'Buy' button") {
        onAddFundsBottomSheet { buyButton.clickWithAssertion() }
    }
    step("Click on 'Confirm' button in 'Dialog'") {
        onDialog { confirmButton.clickWithAssertion() }
    }
    step("Write fiat amount = '$fiatAmount'") {
        onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
    }
}

fun BaseTestCase.openBuyScreenFromTokenContextMenu(tokenTitle: String, fiatAmount: String) {
    step("Long click on token with name: '$tokenTitle'") {
        waitForIdle()
        onMainScreen {
            tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                longClick(durationMillis = CONTEXT_MENU_LONG_CLICK_MS)
            }
        }
    }
    step("Click on 'Buy' button in token context menu") {
        onTokenActionsBottomSheet { buyButton.clickWithAssertion() }
    }
    step("Click on 'Confirm' button in 'Dialog'") {
        onDialog { confirmButton.clickWithAssertion() }
    }
    step("Write fiat amount = '$fiatAmount'") {
        onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
    }
}

fun BaseTestCase.openBuyScreenFromAddFunds(tokenTitle: String, fiatAmount: String) {
    step("Click on 'Add Funds' button") {
        onMainScreen { addFundsButton.clickWithAssertion() }
    }
    step("Click on $tokenTitle in Wallet list") {
        awaitSuccess { onAddFundsBottomSheet { userTokenWithTitle(tokenTitle).clickWithAssertion() } }
    }
    step("Click on 'Buy' button") {
        onAddFundsBottomSheet { buyTokenButton.clickWithAssertion() }
    }
    step("Click on 'Confirm' button in 'Dialog'") {
        onDialog { confirmButton.clickWithAssertion() }
    }
    step("Write fiat amount = '$fiatAmount'") {
        onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
    }
}

fun BaseTestCase.returnFromProviderToOnrampInProgress(
    providerPageUrl: String,
    onrampTransactionId: String,
    providerName: String,
    purchaseDate: String,
    fiatAmountText: String,
    cryptoAmountText: String,
) {
    step("Assert offer token amount is displayed") {
        awaitSuccess { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
    }
    step("Stub opening of the provider page") {
        stubUrlOpening(providerPageUrl)
    }
    step("Click on 'Buy' button and assert provider page is opened") {
        onBuyTokenDetailsScreen {
            buyButton.clickAndWaitFor(composeTestRule) { assertUrlOpened(providerPageUrl, times = 1) }
        }
    }
    step("Cancel the purchase on the provider page and return to the app") {
        openAppByDeepLink("tangem://onramp?result=cancel")
    }
    step("Assert 'Buy' screen is displayed again") {
        awaitSuccess { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
    }
    step("Click on 'Buy' button and assert provider page is opened again") {
        onBuyTokenDetailsScreen {
            buyButton.clickAndWaitFor(composeTestRule) { assertUrlOpened(providerPageUrl, times = 2) }
        }
    }
    step("Return to the app by the provider redirect deeplink") {
        openAppByDeepLink("tangem://onramp?tx_id=$onrampTransactionId")
    }
    step("Assert 'In progress' title is displayed") {
        awaitSuccess { onOnrampSuccessScreen { title.assertIsDisplayed() } }
    }
    step("Assert currency icon is displayed") {
        onOnrampSuccessScreen { currencyIcon.assertIsDisplayed() }
    }
    step("Assert fiat amount is displayed") {
        onOnrampSuccessScreen { textContaining(fiatAmountText).assertIsDisplayed() }
    }
    step("Assert crypto amount is displayed") {
        onOnrampSuccessScreen { textContaining(cryptoAmountText).assertIsDisplayed() }
    }
    step("Assert provider icon is displayed") {
        onOnrampSuccessScreen { providerIcon.assertIsDisplayed() }
    }
    step("Assert provider name '$providerName' is displayed") {
        onOnrampSuccessScreen { providerName(providerName).assertIsDisplayed() }
    }
    step("Assert purchase date is displayed") {
        onOnrampSuccessScreen { textContaining(purchaseDate).assertIsDisplayed() }
    }
    step("Assert transaction status is displayed") {
        onOnrampSuccessScreen { awaitingDepositStatus.assertIsDisplayed() }
    }
    step("Assert transaction status footer is displayed") {
        onOnrampSuccessScreen { footer.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onOnrampSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.openBuyScreenFromMarkets(tokenName: String, fiatAmount: String) {
    step("Open 'Markets' token details of $tokenName") {
        openMarketsTokenDetailsFromMainScreen(tokenName)
    }
    step("Click on 'Add funds' button") {
        awaitSuccess { onMarketsTokenDetailsScreen { addFundsButton.clickWithAssertion() } }
    }
    step("Click on 'Buy' button") {
        awaitSuccess { onAddFundsBottomSheet { buyTokenButton.clickWithAssertion() } }
    }
    step("Click on 'Confirm' button in 'Dialog'") {
        onDialog { confirmButton.clickWithAssertion() }
    }
    step("Write fiat amount = '$fiatAmount'") {
        onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
    }
}