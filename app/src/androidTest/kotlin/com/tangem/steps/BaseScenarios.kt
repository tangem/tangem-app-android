package com.tangem.steps

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.onDisclaimerScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsTooltipScreen
import com.tangem.screens.onStoriesScreen
import com.tangem.tap.domain.sdk.mocks.MockProvider
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openMainScreen(productType: ProductType? = null) {
    if (productType != null) {
        MockProvider.setMocks(productType)
    }
    step("Click on 'Accept' button") {
        onDisclaimerScreen { acceptButton.clickWithAssertion() }
    }
    step("Click on 'Accept' button") {
        onStoriesScreen { scanButton.clickWithAssertion() }
    }
    step("Assert main screen is displayed") {
        onMainScreen { screenContainer.assertIsDisplayed() }
    }
    step("Assert main screen is displayed") {
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