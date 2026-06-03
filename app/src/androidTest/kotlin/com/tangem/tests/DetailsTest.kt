package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.content.Firmware412MockContent
import com.tangem.tap.domain.sdk.mocks.content.S2CMockContent
import com.tangem.tap.domain.sdk.mocks.content.SingleCurrencyMockContent
import com.tangem.tap.domain.sdk.mocks.content.V3MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class DetailsTest : BaseTestCase() {

    @AllureId("836")
    @DisplayName("Details: (Wallet) fields")
    @Test
    fun walletWithoutBackupDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open 'Wallet settings' screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            onWalletSettingsScreen {
                step("Assert 'Link more cards' button is visible") {
                    scrollToLinkMoreCards()
                    linkMoreCardsButton.assertIsDisplayed()
                }
                step("Assert 'Card Settings' button is visible") {
                    scrollToDeviceSettings()
                    deviceSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button is visible") {
                    scrollToReferralProgram()
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    scrollToForgetWallet()
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @AllureId("837")
    @DisplayName("Details: (Note) fields")
    @Test
    fun noteDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(ProductType.Note)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button does not exist") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms or service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open 'Wallet settings' screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            onWalletSettingsScreen {
                step("Assert 'Card Settings' button is visible") {
                    deviceSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button does not exist") {
                    referralProgramButton.assertIsNotDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @AllureId("840")
    @DisplayName("Details: (Twins) fields")
    @Test
    fun twinsDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = ProductType.Twins, isTwinsCard = true)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button does not exist") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Assert app version is visible") {
                    versionName.assertIsDisplayed()
                }
            }
        }

    @AllureId("839")
    @DisplayName("Details: (v4.12) fields")
    @Test
    fun firmware412DetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = Firmware412MockContent)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Assert app version is visible") {
                    versionName.assertIsDisplayed()
                }
            }
        }

    @AllureId("838")
    @DisplayName("Details: (v3 multicurrency) fields")
    @Test
    fun v3MultiCurrencyDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = V3MockContent)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is displayed") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is displayed") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is displayed") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is displayed") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is displayed") {
                    toSButton.assertIsDisplayed()
                }
                step("Assert app version is displayed") {
                    versionName.assertIsDisplayed()
                }
            }
        }

    @AllureId("9832")
    @DisplayName("Details: (single currency) fields")
    @Test
    fun singleCurrencyDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = SingleCurrencyMockContent)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is not displayed") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Buy Tangem card' button is displayed") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is displayed") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is displayed") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is displayed") {
                    toSButton.assertIsDisplayed()
                }
                step("Assert app version is displayed") {
                    versionName.assertIsDisplayed()
                }
            }
        }

    @AllureId("841")
    @DisplayName("Details: (S2C) fields")
    @Test
    fun s2cDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = S2CMockContent)
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is not displayed") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Buy Tangem card' button is displayed") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is displayed") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is displayed") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is displayed") {
                    toSButton.assertIsDisplayed()
                }
                step("Assert app version is displayed") {
                    versionName.assertIsDisplayed()
                }
            }
        }

    // Parked: createWalletActions adds Sell for single-wallet cards with no isStart2Coin() check.
    @Ignore("[REDACTED_JIRA]")
    @AllureId("2869")
    @DisplayName("Details: (S2C) no trade buttons and standard details")
    @Test
    fun s2cNoTradeButtonsDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = S2CMockContent)
            }
            onMainScreen {
                step("Assert 'Buy' button is not displayed") {
                    buyButton.assertIsNotDisplayed()
                }
                step("Assert 'Sell' button is not displayed") {
                    sellButton.assertIsNotDisplayed()
                }
                step("Assert 'Swap' button is not displayed") {
                    swapButton.assertIsNotDisplayed()
                }
            }
            onMainScreenTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is not displayed") {
                    walletConnectButton.assertIsNotDisplayed()
                }
            }
        }

    @AllureId("3647")
    @DisplayName("Referral program: validate screen")
    @Test
    fun validateReferralProgramScreenTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Click on 'Referral program' button ") {
                onWalletSettingsScreen { referralProgramButton.clickWithAssertion() }
            }
            step("Assert 'Referral program' screen title is displayed") {
                onReferralProgramScreen { title.assertIsDisplayed() }
            }
            step("Assert 'Referral program' screen image is displayed") {
                onReferralProgramScreen { image.assertIsDisplayed() }
            }
            step("Assert 'Referral program' screen refer title is displayed") {
                onReferralProgramScreen { referTitle.assertIsDisplayed() }
            }
            step("Assert info for you title is displayed") {
                onReferralProgramScreen { infoForYouText.assertIsDisplayed() }
            }
            step("Assert info for you text is displayed") {
                onReferralProgramScreen { infoForYouBlock.assertIsDisplayed() }
            }
            step("Assert info for your friend title is displayed") {
                onReferralProgramScreen { infoForYourFriendText.assertIsDisplayed() }
            }
            step("Assert info for your friend text is displayed") {
                onReferralProgramScreen { infoForYourFriendBlock.assertIsDisplayed() }
            }
            step("Assert agreement text is displayed") {
                onReferralProgramScreen { agreementText.assertIsDisplayed() }
            }
            step("Assert 'Participate' button is displayed") {
                onReferralProgramScreen { participateButton.assertIsDisplayed() }
            }
        }
}