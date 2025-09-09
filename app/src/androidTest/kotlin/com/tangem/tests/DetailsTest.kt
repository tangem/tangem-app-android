package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onReferralProgramScreen
import com.tangem.screens.onTopBar
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class DetailsTest : BaseTestCase() {

    @Test
    fun walletWithoutBackupDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
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
                    linkMoreCardsButton.assertIsDisplayed()
                }
                step("Assert 'Card Settings' button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    // @Test
    fun wallet2DetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = ProductType.Wallet2, alreadyActivatedDialogIsShown = true)
            }
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
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
                step("Assert 'Link more cards' button does not exist") {
                    linkMoreCardsButton.assertIsNotDisplayed()
                }
                step("Assert 'Card Settings' button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @Test
    fun noteDetailsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(ProductType.Note)
            }
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button does not exist") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
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
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button does not exist") {
                    referralProgramButton.assertIsNotDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
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
                onTopBar { moreButton.clickWithAssertion() }
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