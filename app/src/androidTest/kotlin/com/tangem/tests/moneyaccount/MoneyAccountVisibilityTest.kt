package com.tangem.tests.moneyaccount

import android.Manifest
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.MORALIS_EVM_NFT_API_SCENARIO
import com.tangem.common.constants.TestConstants.REFERRAL_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWcUri
import com.tangem.common.utils.setClipboardText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.scenarios.createConnectionViaPasteFromClipboardButton
import com.tangem.scenarios.enableNFTAndReturnToMain
import com.tangem.scenarios.openAddFundsChooseTokenSheet
import com.tangem.scenarios.openAddTokensPortfolioSelector
import com.tangem.scenarios.openMainWithMoneyAccountAndTwoAccounts
import com.tangem.scenarios.openMarketsAddToPortfolioSelector
import com.tangem.scenarios.openNFTAssetDetails
import com.tangem.scenarios.openNFTCollectionsScreen
import com.tangem.scenarios.openNFTReceivePortfolioSelector
import com.tangem.scenarios.openNFTSendAddressScreen
import com.tangem.scenarios.openReferralPortfolioSelector
import com.tangem.scenarios.openTransferChooseTokenSheet
import com.tangem.scenarios.openWalletConnectScreen
import com.tangem.scenarios.openWalletSettingsScreen
import com.tangem.scenarios.openWcConnectionAccountSelector
import com.tangem.scenarios.setMoneyAccountWithTwoAccountsScenarios
import com.tangem.screens.onAddToPortfolioScreen
import com.tangem.screens.onChooseTokenBottomSheet
import com.tangem.screens.onPortfolioSelectorSheet
import com.tangem.screens.onSendAddressScreen
import com.tangem.screens.onWalletSettingsScreen
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MoneyAccountVisibilityTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val firstAccountName = "Account 1"
    private val secondAccountName = "Account 2"
    private val moneyAccountName = getResourceString(CoreResR.string.money_account_main_name)

    private val moneyAccountDomainName: String
        get() = Account.Money(UserWalletId(stringValue = WALLET_ID_STUB)).accountName.value

    @AllureId("13618")
    @DisplayName("Money account: the account is not offered when adding a token via Markets")
    @Test
    fun moneyAccountNotOfferedWhenAddingTokenViaMarketsTest() {
        val marketsToken = "Bitcoin"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open the portfolio picker for '$marketsToken' in Markets") {
                openMarketsAddToPortfolioSelector(marketsToken)
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onAddToPortfolioScreen { walletOption(firstAccountName).assertIsDisplayed() }
                }
                onAddToPortfolioScreen { walletOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onAddToPortfolioScreen {
                    walletOption(moneyAccountName).assertDoesNotExist()
                    walletOption(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13619")
    @DisplayName("Money account: the account is not offered when adding a token via Add and Manage")
    @Test
    fun moneyAccountNotOfferedWhenAddingTokenViaAddAndManageTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open the account picker behind 'Add tokens'") {
                openAddTokensPortfolioSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onPortfolioSelectorSheet {
                    portfolioOption(moneyAccountName).assertDoesNotExist()
                    portfolioOption(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13620")
    @DisplayName("Money account: the account is not listed among the wallet settings accounts")
    @Test
    fun moneyAccountNotListedInWalletSettingsTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open 'Wallet settings' screen") {
                openWalletSettingsScreen()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onWalletSettingsScreen { accountItem(firstAccountName).assertIsDisplayed() }
                }
                onWalletSettingsScreen { accountItem(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onWalletSettingsScreen {
                    accountItem(moneyAccountName).assertDoesNotExist()
                    accountItem(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13621")
    @DisplayName("Money account: the account is not offered as the referral rewards account")
    @Test
    fun moneyAccountNotOfferedAsReferralRewardsAccountTest() {
        val referralNotParticipatingState = "Started"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Set WireMock scenario '$REFERRAL_API_SCENARIO' to '$referralNotParticipatingState'") {
                setWireMockScenarioState(REFERRAL_API_SCENARIO, referralNotParticipatingState)
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open the referral 'Account for rewards' picker") {
                openReferralPortfolioSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onPortfolioSelectorSheet {
                    portfolioOption(moneyAccountName).assertDoesNotExist()
                    portfolioOption(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13622")
    @DisplayName("Money account: the account is not offered when connecting a dApp via WalletConnect")
    @Test
    fun moneyAccountNotOfferedWhenConnectingDAppViaWalletConnectTest() {
        val context = device.context
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            },
        ).run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            step("Set WalletConnect URI to clipboard") {
                setClipboardText(context, getWcUri())
            }
            step("Create connection via 'Paste from clipboard' button") {
                createConnectionViaPasteFromClipboardButton()
            }
            step("Open the account selector on the connection sheet") {
                openWcConnectionAccountSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onPortfolioSelectorSheet {
                    portfolioOption(moneyAccountName).assertDoesNotExist()
                    portfolioOption(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13623")
    @DisplayName("Money account: the account is not offered when receiving an NFT")
    @Test
    fun moneyAccountNotOfferedWhenReceivingNFTTest() {
        val nftEmptyState = "Started"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Set WireMock scenario '$MORALIS_EVM_NFT_API_SCENARIO' to '$nftEmptyState'") {
                setWireMockScenarioState(MORALIS_EVM_NFT_API_SCENARIO, nftEmptyState)
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Enable NFT in wallet settings") {
                enableNFTAndReturnToMain()
            }
            step("Open 'NFT collections' screen") {
                openNFTCollectionsScreen()
            }
            step("Open the account picker behind 'Receive NFT'") {
                openNFTReceivePortfolioSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onPortfolioSelectorSheet {
                    portfolioOption(moneyAccountName).assertDoesNotExist()
                    portfolioOption(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13624")
    @DisplayName("Money account: the account is not listed among the NFT send accounts")
    @Test
    fun moneyAccountNotListedInNFTSendAccountsTest() {
        val nftSingleCollectionState = "SingleCollection"
        val nftCollectionName = "Tangem Test Collection"
        val nftAssetName = "Tangem Test NFT #1"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Set WireMock scenario '$MORALIS_EVM_NFT_API_SCENARIO' to '$nftSingleCollectionState'") {
                setWireMockScenarioState(MORALIS_EVM_NFT_API_SCENARIO, nftSingleCollectionState)
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Enable NFT in wallet settings") {
                enableNFTAndReturnToMain()
            }
            step("Open 'NFT collections' screen") {
                openNFTCollectionsScreen()
            }
            step("Open '$nftAssetName' asset details") {
                openNFTAssetDetails(collectionName = nftCollectionName, assetName = nftAssetName)
            }
            step("Open the send 'Address' screen") {
                openNFTSendAddressScreen()
            }
            step("Assert '$secondAccountName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSendAddressScreen { suggestedAccountItem(secondAccountName).assertIsDisplayed() }
                }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onSendAddressScreen {
                    suggestedAccountItem(moneyAccountName).assertDoesNotExist()
                    suggestedAccountItem(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13625")
    @DisplayName("Money account: the account is not listed in the wallet Add funds token selector")
    @Test
    fun moneyAccountNotListedInAddFundsSelectorTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open the 'Choose token' sheet from 'Add funds'") {
                openAddFundsChooseTokenSheet()
            }
            step("Assert '$firstAccountName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onChooseTokenBottomSheet { accountHeader(firstAccountName).assertIsDisplayed() }
                }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onChooseTokenBottomSheet {
                    accountHeader(moneyAccountName).assertDoesNotExist()
                    accountHeader(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13626")
    @DisplayName("Money account: the account is not listed in the wallet Transfers token selector")
    @Test
    fun moneyAccountNotListedInTransfersSelectorTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Money account wallet with two accounts") {
                setMoneyAccountWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Money account wallet holding two accounts") {
                openMainWithMoneyAccountAndTwoAccounts()
            }
            step("Open the 'Choose token' sheet from 'Transfer'") {
                openTransferChooseTokenSheet()
            }
            step("Assert '$firstAccountName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onChooseTokenBottomSheet { accountHeader(firstAccountName).assertIsDisplayed() }
                }
            }
            step("Assert '$moneyAccountName' is not displayed") {
                onChooseTokenBottomSheet {
                    accountHeader(moneyAccountName).assertDoesNotExist()
                    accountHeader(moneyAccountDomainName).assertDoesNotExist()
                }
            }
        }
    }

    private companion object {
        const val WALLET_ID_STUB = "00"
    }
}