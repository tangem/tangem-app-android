package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_TANGEM_PAY_ORDER_POLL
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_WALLET_CREATION
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.scenarios.assertTangemPayCampaignIssueCardContent
import com.tangem.scenarios.assertTangemPayCampaignLandingContent
import com.tangem.scenarios.countTangemPayCampaignOrders
import com.tangem.scenarios.countTangemPayDeeplinkValidations
import com.tangem.scenarios.exitTangemPayKycScreen
import com.tangem.scenarios.openMainScreenWithNewHotWallet
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.refreshMainUntilTangemPayTileSubtitle
import com.tangem.scenarios.refreshTangemPayUntilIssuingBannerIsGone
import com.tangem.scenarios.restoreTangemPayMockOnboardingOrder
import com.tangem.scenarios.selectTangemPayResidency
import com.tangem.scenarios.serveTangemPayOnboardingOrderFromWireMock
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onIntroductionScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.onTangemPayCampaignScreen
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class TangemPayCampaignCardTest : BaseTestCase() {

    @Inject
    lateinit var appsFlyerRepository: AppsFlyerRepository

    private val campaignKey = "tpay_for_unical1"
    private val validatedCampaignLink = "tangem://marketing/$campaignKey"
    private val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)
    private val issuingYourCardText = getResourceString(CoreResR.string.tangempay_issuing_your_card)
    private val residencyCountry = "United States"
    private val residencyRegion = "California"

    @AllureId("11647")
    @DisplayName("Tangem Pay: the campaign link opened without a wallet issues the promo card")
    @Test
    fun campaignCardIsOrderedWithoutWalletTest() {
        val eligibilityStartedState = "Started"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val issueOrdersScenario = "tangem_pay_card_issue_orders"
        val issueOrdersEmptyState = "Empty"
        val campaignCardScenario = "tangem_pay_campaign_card"
        val kycApprovedIssuingState = "KycApprovedIssuing"

        setupHooks(
            additionalBeforeSection = { serveTangemPayOnboardingOrderFromWireMock() },
            additionalAfterSection = { restoreTangemPayMockOnboardingOrder() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Set WireMock scenario '$issueOrdersScenario' to '$issueOrdersEmptyState'") {
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
            step("Assert 'Introduction' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onIntroductionScreen { createWalletButton.assertIsDisplayed() }
                }
            }
            val validationsBefore = countTangemPayDeeplinkValidations(validatedCampaignLink)
            val ordersBefore = countTangemPayCampaignOrders(campaignKey)
            // The AppsFlyer SDK never calls back in tests, so the key it would store is written directly.
            step("Store the AppsFlyer campaign deep link '$campaignKey'") {
                runBlocking { appsFlyerRepository.storeNavigationDeeplink(campaignKey) }
            }
            step("Assert campaign landing content") { assertTangemPayCampaignLandingContent() }
            step("Assert 'Close' button is not displayed") {
                onTangemPayCampaignScreen { closeButton.assertIsNotDisplayed() }
            }
            step("Assert Terms of Service and Privacy Policy caption is displayed") {
                onTangemPayCampaignScreen { legalCaption.assertIsDisplayed() }
            }
            step("Assert the campaign link '$validatedCampaignLink' is validated once") {
                assertEquals(1, countTangemPayDeeplinkValidations(validatedCampaignLink) - validationsBefore)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on 'Get card' button") {
                onTangemPayCampaignScreen { getCardButton.clickWithAssertion() }
            }
            step("Select residency: '$residencyCountry', '$residencyRegion'") {
                selectTangemPayResidency(
                    country = residencyCountry,
                    region = residencyRegion,
                    timeoutMillis = WAIT_UNTIL_TIMEOUT_WALLET_CREATION,
                )
            }
            step("Assert one card order is placed with the campaign key '$campaignKey'") {
                assertEquals(1, countTangemPayCampaignOrders(campaignKey) - ordersBefore)
            }
            step("Leave the KYC screen") { exitTangemPayKycScreen() }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Set WireMock scenario '$campaignCardScenario' to '$kycApprovedIssuingState'") {
                setWireMockScenarioState(campaignCardScenario, kycApprovedIssuingState)
            }
            step("Wait for the card issuing status on Main") {
                refreshMainUntilTangemPayTileSubtitle(issuingYourCardText)
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert 'Issuing your card' sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { issuingCardSheetDescription.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("11648")
    @DisplayName("Tangem Pay: the campaign link opened in a wallet without Tangem Pay issues the promo card")
    @Test
    fun campaignCardIsOrderedForWalletWithoutTangemPayTest() {
        val eligibilityStartedState = "Started"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val issueOrdersScenario = "tangem_pay_card_issue_orders"
        val issueOrdersEmptyState = "Empty"
        val campaignCardScenario = "tangem_pay_campaign_card"
        val kycApprovedIssuingState = "KycApprovedIssuing"
        val cardIssuedState = "CardIssued"
        val issueOrderScenario = "tangem_pay_card_issue_order"
        val issueOrderCompletedState = "Completed"
        val oneCardSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 1, 1)

        setupHooks(
            additionalBeforeSection = { serveTangemPayOnboardingOrderFromWireMock() },
            additionalAfterSection = { restoreTangemPayMockOnboardingOrder() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Set WireMock scenario '$issueOrdersScenario' to '$issueOrdersEmptyState'") {
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
            step("Create a new mobile wallet and open 'Main' screen") {
                openMainScreenWithNewHotWallet()
            }
            val validationsBefore = countTangemPayDeeplinkValidations(validatedCampaignLink)
            val ordersBefore = countTangemPayCampaignOrders(campaignKey)
            step("Store the AppsFlyer campaign deep link '$campaignKey'") {
                runBlocking { appsFlyerRepository.storeNavigationDeeplink(campaignKey) }
            }
            step("Assert campaign landing content") { assertTangemPayCampaignLandingContent() }
            step("Assert 'Close' button is displayed") {
                onTangemPayCampaignScreen { closeButton.assertIsDisplayed() }
            }
            step("Assert Terms of Service and Privacy Policy caption is not displayed") {
                onTangemPayCampaignScreen { legalCaption.assertIsNotDisplayed() }
            }
            step("Assert the campaign link '$validatedCampaignLink' is validated once") {
                assertEquals(1, countTangemPayDeeplinkValidations(validatedCampaignLink) - validationsBefore)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Click on 'Get card' button") {
                onTangemPayCampaignScreen { getCardButton.clickWithAssertion() }
            }
            step("Select residency: '$residencyCountry', '$residencyRegion'") {
                selectTangemPayResidency(country = residencyCountry, region = residencyRegion)
            }
            step("Assert one card order is placed with the campaign key '$campaignKey'") {
                assertEquals(1, countTangemPayCampaignOrders(campaignKey) - ordersBefore)
            }
            step("Leave the KYC screen") { exitTangemPayKycScreen() }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Set WireMock scenario '$campaignCardScenario' to '$kycApprovedIssuingState'") {
                setWireMockScenarioState(campaignCardScenario, kycApprovedIssuingState)
            }
            step("Wait for the card issuing status on Main") {
                refreshMainUntilTangemPayTileSubtitle(issuingYourCardText)
            }
            step("Set WireMock scenario '$campaignCardScenario' to '$cardIssuedState'") {
                setWireMockScenarioState(campaignCardScenario, cardIssuedState)
            }
            step("Set WireMock scenario '$issueOrderScenario' to '$issueOrderCompletedState'") {
                setWireMockScenarioState(issueOrderScenario, issueOrderCompletedState)
            }
            step("Assert Tangem Pay tile with '$oneCardSubtitle' is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_TANGEM_PAY_ORDER_POLL) {
                    onTangemPayMainScreen { tileWithSubtitle(oneCardSubtitle).assertIsDisplayed() }
                }
            }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert only one wallet is displayed on 'Details' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onDetailsScreen {
                        walletItemAt(0).assertIsDisplayed()
                        walletItemAt(1).assertDoesNotExist()
                    }
                }
            }
        }
    }

    @AllureId("11649")
    @DisplayName("Tangem Pay: the campaign link issues an additional promo card on an active Payment account")
    @Test
    fun additionalCampaignCardIsIssuedOnActivePaymentAccountTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val issueOrderScenario = "tangem_pay_card_issue_order"
        val issueOrderStartedState = "Started"
        val issueOrderCompletedState = "Completed"
        val issueOrdersScenario = "tangem_pay_card_issue_orders"
        val issueOrdersEmptyState = "Empty"
        val cardIssueScenario = "tangem_pay_card_issue"
        val twoCardsState = "TwoCards"
        val issueFee = "$1"
        val issuedCardIndex = 1

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Set WireMock scenario '$issueOrderScenario' to '$issueOrderStartedState'") {
                setWireMockScenarioState(issueOrderScenario, issueOrderStartedState)
            }
            step("Set WireMock scenario '$issueOrdersScenario' to '$issueOrdersEmptyState'") {
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
            step("Open Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            val ordersBefore = countTangemPayCampaignOrders(campaignKey)
            step("Store the AppsFlyer campaign deep link '$campaignKey'") {
                runBlocking { appsFlyerRepository.storeNavigationDeeplink(campaignKey) }
            }
            step("Assert 'Issue special card' content with the '$issueFee' fee") {
                assertTangemPayCampaignIssueCardContent(issueFee)
            }
            step("Click on 'Issue' button") {
                onTangemPayCampaignScreen { issueButton.clickWithAssertion() }
            }
            step("Assert 'Issuing a new digital card' banner is displayed on the payment account") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { issuingCardBanner.assertIsDisplayed() }
                }
            }
            step("Assert one card order is placed with the campaign key '$campaignKey'") {
                assertEquals(1, countTangemPayCampaignOrders(campaignKey) - ordersBefore)
            }
            step("Set WireMock scenario '$cardIssueScenario' to '$twoCardsState'") {
                setWireMockScenarioState(cardIssueScenario, twoCardsState)
            }
            step("Set WireMock scenario '$issueOrderScenario' to '$issueOrderCompletedState'") {
                setWireMockScenarioState(issueOrderScenario, issueOrderCompletedState)
            }
            step("Set WireMock scenario '$issueOrdersScenario' to '$issueOrdersEmptyState'") {
                setWireMockScenarioState(issueOrdersScenario, issueOrdersEmptyState)
            }
            step("Wait for the card issue to complete") { refreshTangemPayUntilIssuingBannerIsGone() }
            step("Click on the issued card") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { cardButtonAt(issuedCardIndex).assertIsDisplayed() }
                }
                onTangemPayMainScreen { cardButtonAt(issuedCardIndex).performClick() }
            }
            step("Assert the card page is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayCardPageScreen { moreButton.assertIsDisplayed() }
                }
            }
        }
    }
}