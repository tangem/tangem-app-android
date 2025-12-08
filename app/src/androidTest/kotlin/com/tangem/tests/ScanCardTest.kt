package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.checkMultiCurrencyMainScreen
import com.tangem.scenarios.checkSingleCurrencyMainScreen
import com.tangem.scenarios.openMainScreen
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.content.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class ScanCardTest : BaseTestCase() {

    @ApiEnv(
        ApiEnvConfig(ApiConfig.ID.TangemTech, ApiEnvironment.PROD),
        ApiEnvConfig(ApiConfig.ID.Express, ApiEnvironment.PROD)
    )
    @AllureId("868")
    @DisplayName("Scan: Scanning single-currency cards")
    @Test
    fun singleTokenNoteCardScanTest() {
        val cardBlockchain = "DOGE"
        val cardType: ProductType = ProductType.Note

        setupHooks().run {
                step("Open 'Main Screen' on '${cardType.name}' card") {
                    openMainScreen(cardType)
                }
                step("Check 'Main' screen for '${cardType.name}' $cardBlockchain card") {
                    checkSingleCurrencyMainScreen(
                        cardBlockchain = cardBlockchain,
                        cardTitle = cardType.name,
                        withTransactions = true
                    )
                }
        }
    }

    @AllureId("869")
    @DisplayName("Scan: 'Twin' card")
    @Test
    fun twinsCardScanTest() {
        val cardBlockchain = "BTC"
        val cardType: MockContent = TwinsMockContent
        val cardName = "Twin"

        setupHooks().run {
            step("Open 'Main Screen' on '$cardName' card") {
                openMainScreen(mockContent = cardType, isTwinsCard = true)
            }
            step("Check 'Main' screen for '$cardName' $cardBlockchain card") {
                checkSingleCurrencyMainScreen(cardBlockchain = cardBlockchain, cardTitle = cardName)
            }
        }
    }

    @AllureId("872")
    @DisplayName("Scan: Card with Secp256k1 curve")
    @Test
    fun secpk1CurveCardScanTest() {
        val devicesCount = "1 device"
        val cardType: MockContent = Secpk1CurveMockContent
        val cardName = "Wallet"
        val card = "card with Secp256k1 curve"

        setupHooks().run {
            step("Open 'Main Screen' on $card") {
                openMainScreen(mockContent = cardType, alreadyActivatedDialogIsShown = true)
            }
            step("Check 'Main' screen for $card curve with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(
                    devicesCount = devicesCount,
                    cardTitle = cardName,
                    withWalletImage = false
                )
            }
        }
    }

    @AllureId("870")
    @DisplayName("Scan: Card with Ed25519 curve")
    @Test
    fun edCurveCardScanTest() {
        val cardBlockchain = "XLM"
        val cardType: MockContent = EdCurveMockContent
        val cardName = "Tangem card"
        val card = "card with Ed25519 curve"

        setupHooks().run {
            step("Open 'Main Screen' on $card") {
                openMainScreen(mockContent = cardType)
            }
            step("Check 'Main' screen for $card with blockchain: '$cardBlockchain'") {
                checkSingleCurrencyMainScreen(
                    cardBlockchain = cardBlockchain,
                    cardTitle = cardName,
                    withWalletImage = false
                )
            }
        }
    }

    @AllureId("866")
    @DisplayName("Scan: 'Shiba' card")
    @Test
    fun shibaCardScanTest() {
        val devicesCount = "2 devices"
        val cardType: MockContent = ShibaMockContent
        val cardName = "Wallet"
        val card = "Shiba"

        setupHooks().run {
            step("Open 'Main Screen' on '$card' card") {
                openMainScreen(mockContent = cardType, alreadyActivatedDialogIsShown = true)
            }
            step("Check 'Main' screen for '$card' card with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(devicesCount, cardName)
            }
        }
    }

    @AllureId("864")
    @DisplayName("Scan: 'Ring'")
    @Test
    fun ringScanTest() {
        val devicesCount = "3 devices"
        val cardType: ProductType = ProductType.Ring
        val cardName = "Wallet"
        val ring = "Ring"

        setupHooks().run {
            step("Open 'Main Screen' on '$ring'") {
                openMainScreen(productType = cardType, alreadyActivatedDialogIsShown = true)
            }
            step("Check 'Main' screen for '$ring' with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(devicesCount, cardName)
            }
        }
    }

    @AllureId("867")
    @DisplayName("Scan: 'Wallet' card")
    @Test
    fun walletCardScanTest() {
        val devicesCount = "1 device"
        val cardType: ProductType = ProductType.Wallet
        val cardName = "Wallet"

        setupHooks().run {
            step("Open 'Main Screen' on '$cardName' card") {
                openMainScreen(productType = cardType)
            }
            step("Check 'Main' screen for '$cardName' card with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(devicesCount, cardName)
            }
        }
    }

    @AllureId("865")
    @DisplayName("Scan: 'Wallet 2' card")
    @Test
    fun wallet2ScanTest() {
        val devicesCount = "2 devices"
        val cardType: MockContent = Wallet2MockContent
        val cardName = "Wallet"
        val card = "Wallet 2"

        setupHooks().run {
            step("Open 'Main Screen' on '$card' card") {
                openMainScreen(mockContent = cardType, alreadyActivatedDialogIsShown = true)
            }
            step("Check 'Main' screen for '$card' card with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(devicesCount, cardName)
            }
        }
    }

    @AllureId("871")
    @DisplayName("Scan: Card with 4.12 firmware")
    @Test
    fun firmware412CardScanTest() {
        val devicesCount = "1 device"
        val cardType: MockContent = Firmware412MockContent
        val cardName = "Tangem card"
        val card = "card with 4.12 firmware"

        setupHooks().run {
            step("Open 'Main Screen' on '$card'") {
                openMainScreen(mockContent = cardType)
            }
            step("Check 'Main' screen for '$card' with devices count = '$devicesCount'") {
                checkMultiCurrencyMainScreen(devicesCount, cardName)
            }
        }
    }
}