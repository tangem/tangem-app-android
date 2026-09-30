package com.tangem.tests.feed

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.expandFeed
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.accounts.onAccountInfoEditorScreen
import com.tangem.screens.accounts.onJointAccountCompositionScreen
import com.tangem.screens.accounts.onJointAccountConfigScreen
import com.tangem.screens.accounts.onJointAccountDisplayNameScreen
import com.tangem.screens.accounts.onJointAccountMembersScreen
import com.tangem.screens.accounts.onJointAccountPromoScreen
import com.tangem.screens.onAddAccountTypeBottomSheet
import com.tangem.screens.onManageTokensScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.onPortfolioSelectorSheet
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class FeedAddFundsTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1611_JOINT_ACCOUNT_ENABLED.rawName to true,
    )

    private val userTokensScenario = "user_tokens_api"

    @Test
    @AllureId("11634")
    @DisplayName("Feed: Add funds. Available account types with a single wallet")
    fun availableAccountTypesWithSingleWalletTest() {
        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Click on 'Add account' tile") {
                onMarketsScreen { addAccountBanner.performClick() }
            }
            step("Assert 'Crypto account' option is displayed") {
                flakySafely {
                    onAddAccountTypeBottomSheet {
                        cryptoAccountButton.assertIsDisplayed()
                        cryptoAccountButton.assertHasClickAction()
                    }
                }
            }
            step("Assert 'Joint account' option is displayed") {
                onAddAccountTypeBottomSheet {
                    jointAccountButton.assertIsDisplayed()
                    jointAccountButton.assertHasClickAction()
                }
            }
        }
    }

    @Test
    @AllureId("11635")
    @DisplayName("Feed: Add funds. Wallet chooser with several wallets")
    fun walletChooserWithSeveralWalletsTest() {
        val firstWalletName = "Wallet"
        val secondWalletName = "Wallet 2"

        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Add second wallet '$secondWalletName'") {
                addNewCardWallet(Wallet2WithDerivationsMockContent)
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Click on 'Add account' tile") {
                onMarketsScreen { addAccountBanner.performClick() }
            }
            step("Assert wallet chooser lists both wallets") {
                flakySafely {
                    onPortfolioSelectorSheet {
                        portfolioOption(firstWalletName).assertIsDisplayed()
                        portfolioOption(secondWalletName).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @Test
    @AllureId("11636")
    @DisplayName("Feed: Add funds. Create a crypto account")
    fun createCryptoAccountTest() {
        val accountName = "Account 2"
        val accountReadyState = "AccountReadyToCreateEmpty"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, accountReadyState)
            },
        ).run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Click on 'Add account' tile") {
                onMarketsScreen { addAccountBanner.performClick() }
            }
            step("Click on 'Crypto account' option") {
                flakySafely { onAddAccountTypeBottomSheet { cryptoAccountButton.performClick() } }
            }
            step("Enter account name '$accountName'") {
                onAccountInfoEditorScreen {
                    accountNameField.performClick()
                    accountNameField.performTextInput(accountName)
                }
            }
            step("Click on 'Create account' button") {
                onAccountInfoEditorScreen { saveAccountButton.clickWithAssertion() }
            }
            step("Assert 'Manage tokens' screen is displayed") {
                onManageTokensScreen { topAppBarTitle.assertIsDisplayed() }
            }
            step("Close 'Manage tokens' screen") {
                onManageTokensScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert account '$accountName' is displayed on 'Main' screen") {
                flakySafely { onMainScreen { findAccountSectionByName(accountName).assertIsDisplayed() } }
            }
        }
    }

    @Test
    @AllureId("11637")
    @DisplayName("Feed: Add funds. Create a joint account")
    fun createJointAccountTest() {
        val jointAccountName = "Wallet 2 Joint"
        val creatorName = "Autotest"
        val jointAccountReadyState = "JointAccountReadyToCreate"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(userTokensScenario, jointAccountReadyState)
            },
        ).run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Click on 'Add account' tile") {
                onMarketsScreen { addAccountBanner.performClick() }
            }
            step("Click on 'Joint account' option") {
                flakySafely { onAddAccountTypeBottomSheet { jointAccountButton.performClick() } }
            }
            step("Click on 'Continue' on the joint promo screen") {
                flakySafely { onJointAccountPromoScreen { continueButton.assertIsDisplayed() } }
                onJointAccountPromoScreen { continueButton.performClick() }
            }
            step("Enter joint account name '$jointAccountName'") {
                flakySafely { onJointAccountConfigScreen { nameField.assertIsDisplayed() } }
                onJointAccountConfigScreen {
                    nameField.performClick()
                    nameField.performTextReplacement(jointAccountName)
                }
            }
            step("Click on 'Continue' on the config screen") {
                onJointAccountConfigScreen { continueButton.performClick() }
            }
            step("Click on 'Continue' on the members screen") {
                flakySafely { onJointAccountCompositionScreen { continueButton.assertIsDisplayed() } }
                onJointAccountCompositionScreen { continueButton.performClick() }
            }
            step("Enter your display name '$creatorName'") {
                flakySafely { onJointAccountDisplayNameScreen { nameField.assertIsDisplayed() } }
                onJointAccountDisplayNameScreen {
                    nameField.performClick()
                    nameField.performTextInput(creatorName)
                }
            }
            step("Click on 'Create account' button") {
                onJointAccountDisplayNameScreen { createAccountButton.performClick() }
            }
            step("Close 'Invite members' screen") {
                flakySafely { onJointAccountMembersScreen { closeButton.assertIsDisplayed() } }
                onJointAccountMembersScreen { closeButton.performClick() }
            }
            step("Assert joint account '$jointAccountName' is displayed on 'Main' screen") {
                flakySafely { onMainScreen { findAccountSectionByName(jointAccountName).assertIsDisplayed() } }
            }
        }
    }
}