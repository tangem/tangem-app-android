package com.tangem.scenarios

import com.atiurin.ultron.allure.step.step
import com.atiurin.ultron.extensions.assertIsDisplayed
import com.atiurin.ultron.extensions.click
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.DisclaimerPage
import com.tangem.screens.MainPage
import com.tangem.screens.StoriesPage
import com.tangem.tap.domain.sdk.mocks.MockProvider

object MainPageScenario {
    fun open(productType: ProductType? = null) {
        if (productType != null) {
            MockProvider.setMocks(productType)
        }
        step("Click on \"Accept\" button") {
            DisclaimerPage.acceptButton.click()
        }
        step("Click on \"Scan\" button emulating scan error") {
            StoriesPage.scanButton.click()
        }
        step("Assert: main is displayed") {
            MainPage.container.assertIsDisplayed()
        }
    }
}