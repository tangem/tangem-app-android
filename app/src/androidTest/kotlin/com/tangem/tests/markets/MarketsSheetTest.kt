package com.tangem.tests.markets

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMarketsScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.content.ShibaMockContent
import com.tangem.tap.domain.sdk.mocks.content.TwinsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MarketsSheetTest : BaseTestCase() {

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("3597")
    @DisplayName("Markets: verify Markets sheet is displayed (Wallet 2 card)")
    fun marketsSheetDisplayedOnWallet2Test() = runMarketsSheetTest(
        cardName = "Wallet 2",
        mockContent = Wallet2MockContent,
    )

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("3597")
    @DisplayName("Markets: verify Markets sheet is displayed (Wallet card)")
    fun marketsSheetDisplayedOnWalletTest() = runMarketsSheetTest(
        cardName = "Wallet",
        productType = ProductType.Wallet,
    )

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("3597")
    @DisplayName("Markets: verify Markets sheet is displayed (Twin card)")
    fun marketsSheetDisplayedOnTwinTest() = runMarketsSheetTest(
        cardName = "Twin",
        mockContent = TwinsMockContent,
        isTwinsCard = true,
    )

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("3597")
    @DisplayName("Markets: verify Markets sheet is displayed (Shiba card)")
    fun marketsSheetDisplayedOnShibaTest() = runMarketsSheetTest(
        cardName = "Shiba",
        mockContent = ShibaMockContent,
    )

    @Test
    @ApiEnv(ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD))
    @AllureId("3597")
    @DisplayName("Markets: verify Markets sheet is displayed (Ring)")
    fun marketsSheetDisplayedOnRingTest() = runMarketsSheetTest(
        cardName = "Ring",
        productType = ProductType.Ring,
    )

    private fun runMarketsSheetTest(
        cardName: String,
        productType: ProductType? = null,
        mockContent: MockContent? = null,
        isTwinsCard: Boolean = false,
    ) {
        setupHooks().run {
            step("Open 'Markets' sheet on '$cardName' card") {
                openMarketsScreen(
                    productType = productType,
                    mockContent = mockContent,
                    isTwinsCard = isTwinsCard,
                )
            }
            step("Assert 'Markets' sheet is displayed (search field is visible)") {
                onMarketsScreen {
                    searchThroughMarketPlaceholder.assertIsDisplayed()
                }
            }
        }
    }
}