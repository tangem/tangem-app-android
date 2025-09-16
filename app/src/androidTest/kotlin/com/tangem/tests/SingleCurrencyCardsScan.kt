package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.steps.checkSingleCurrencyMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SingleCurrencyCardsScan : BaseTestCase() {

    @AllureId("868")
    @DisplayName("Scan: Scanning single-currency cards")
    @Test
    fun singleTokenNoteScanTest() {
        val cardBlockchain = "DOGE"
        val cardType: ProductType = ProductType.Note

        setupHooks().run {
                step("Open 'Main Screen' on ${cardType.name} card") {
                    openMainScreen(cardType)
                }
                step("Check 'Main' screen for ${cardType.name} $cardBlockchain card") {
                    checkSingleCurrencyMainScreen(cardBlockchain = cardBlockchain, cardTitle = cardType.name)
                }
        }
    }
}