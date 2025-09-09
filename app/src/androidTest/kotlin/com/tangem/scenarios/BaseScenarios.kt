package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockProvider
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openMainScreen(productType: ProductType? = null, alreadyActivatedDialogIsShown : Boolean = false) {
    if (productType != null) {
        MockProvider.setMocks(productType)
    }
    step("Click on 'Accept' button") {
        onDisclaimerScreen { acceptButton.clickWithAssertion() }
    }
    step("Click on 'Scan' button") {
        onStoriesScreen { scanButton.clickWithAssertion() }
    }
    if (alreadyActivatedDialogIsShown) {
        step("Click on 'This is my wallet' button") {
            AlreadyUsedWalletDialogPageObject { thisIsMyWalletButton.click() }
        }
    }
    step("Assert 'Main' screen is displayed") {
        onMainScreen { screenContainer.assertIsDisplayed() }
    }
    step("Click on 'Market Tooltip' screen") {
        onMarketsTooltipScreen { contentContainer.clickWithAssertion() }
    }
}

fun BaseTestCase.synchronizeAddresses(balance: String) {
    step("Click on 'Synchronize addresses' button") {
        onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
    }
    step("Assert wallet balance = '$balance'") {
        onMainScreen { walletBalance().assertTextContains(balance) }
    }
}