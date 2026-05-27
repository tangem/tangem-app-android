package com.tangem.tests.swap

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.onMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SwapMainScreenTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD),
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("574")
    @DisplayName("Swap: 'Swap' button is not displayed for single currency card")
    @Test
    fun singleTokenNoteCardScanTest() {
        val cardType: ProductType = ProductType.Note

        setupHooks().run {
            step("Open 'Main Screen' on '${cardType.name}' card") {
                openMainScreen(cardType)
            }
            step("Assert 'Swap' button is not displayed") {
                onMainScreen { swapButton.assertIsNotDisplayed() }
            }
        }
    }
}