package com.tangem.scenarios

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.screens.DisclaimerTestScreen
import com.tangem.screens.MainTestScreen
import com.tangem.screens.StoriesTestScreen
import com.tangem.screens.TestTopBar
import com.tangem.tap.domain.sdk.mocks.MockProvider
import io.github.kakaocup.compose.node.element.ComposeScreen

class OpenMainScreenScenario(
    private val testRule: ComposeTestRule,
    private val productType: ProductType? = null,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        if (productType != null) {
            MockProvider.setMocks(productType)
        }
        ComposeScreen.onComposeScreen<DisclaimerTestScreen>(testRule) {
            step("Click on \"Accept\" button") {
                acceptButton.clickWithAssertion()
            }
        }
        ComposeScreen.onComposeScreen<StoriesTestScreen>(testRule) {
            step("Click on \"Scan\" button") {
                scanButton.clickWithAssertion()
            }
        }
        ComposeScreen.onComposeScreen<MainTestScreen>(testRule) {
            step("Make sure wallet screen is visible") {
                assertIsDisplayed()
            }
        }
        ComposeScreen.onComposeScreen<TestTopBar>(testRule) {
            step("Close Markets tooltip"){
                performClick()
            }
        }
    }
}