package com.tangem.tests.polymarket

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertSnackbarWithText
import com.tangem.common.utils.findWireMockRequestQueryParamValues
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.POLYMARKET_AB_TEST_ON
import com.tangem.scenarios.POLYMARKET_AB_TEST_PROPERTY
import com.tangem.scenarios.POLYMARKET_APPROVALS_PATH
import com.tangem.scenarios.POLYMARKET_BALANCE_PATH
import com.tangem.scenarios.POLYMARKET_BALANCE_SCENARIO
import com.tangem.scenarios.POLYMARKET_CREATED_API_KEY
import com.tangem.scenarios.POLYMARKET_DERIVED_API_KEY
import com.tangem.scenarios.POLYMARKET_CREATE_KEY_PATH
import com.tangem.scenarios.POLYMARKET_CREDENTIALS_SCENARIO
import com.tangem.scenarios.POLYMARKET_DEPLOY_PATH
import com.tangem.scenarios.POLYMARKET_DERIVE_KEY_PATH
import com.tangem.scenarios.POLYMARKET_OWNER_ADDRESS
import com.tangem.scenarios.POLYMARKET_RELAYER_NONCE_PATH
import com.tangem.scenarios.POLYMARKET_STORIES_SCENARIO
import com.tangem.scenarios.POLYMARKET_WALLET_ID
import com.tangem.scenarios.POLYMARKET_WALLET_PATH
import com.tangem.scenarios.POLYMARKET_WALLET_SCENARIO
import com.tangem.scenarios.addNewCardWalletWithoutSync
import com.tangem.scenarios.assertPolymarketApprovalsCalls
import com.tangem.scenarios.assertPolymarketApprovalsDeadline
import com.tangem.scenarios.assertPolymarketApprovalsNonce
import com.tangem.scenarios.assertPolymarketApprovalsSignature
import com.tangem.scenarios.assertPolymarketApprovalsRequestMatchesWallet
import com.tangem.scenarios.assertPolymarketDeployRequestMatchesWallet
import com.tangem.scenarios.assertPolymarketL1HeadersSent
import com.tangem.scenarios.assertPolymarketL2CredentialsSent
import com.tangem.scenarios.assertPolymarketRelayerNonceRequested
import com.tangem.scenarios.assertPolymarketRequestOrder
import com.tangem.scenarios.awaitPolymarketMainScreen
import com.tangem.scenarios.checkPolymarketOnboardingStories
import com.tangem.scenarios.clickPolymarketOnboardingActionButton
import com.tangem.scenarios.openMainScreenWithPolymarketHotWallet
import com.tangem.scenarios.openPolymarketFromMarketsBanner
import com.tangem.screens.onPolymarketOnboardingScreen
import com.tangem.screens.onSwapStoriesScreen
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.tangem.core.res.R as CoreResR

@HiltAndroidTest
class PolymarketOnboardingTest : BaseTestCase() {

    @Test
    @AllureId("12918")
    @DisplayName("Polymarket: onboarding. Successful completion opens the account main screen")
    fun onboardingCompletedTest() {
        val storiesState = "Available"
        val toastText = getResourceString(CoreResR.string.account_create_success_message)

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            step("Set WireMock scenario '$POLYMARKET_STORIES_SCENARIO' to '$storiesState'") {
                setWireMockScenarioState(POLYMARKET_STORIES_SCENARIO, storiesState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert onboarding stories of the new account are displayed") {
                checkPolymarketOnboardingStories()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert 'Account created' toast is displayed") {
                assertSnackbarWithText(toastText)
            }
        }
    }

    @Test
    @AllureId("12899")
    @DisplayName("Polymarket: onboarding. Account status is requested by wallet id, then by owner address")
    fun accountStatusRequestTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val byWalletIdBefore = getWireMockRequestCountByQueryParam(
                "GET", POLYMARKET_WALLET_PATH, "walletId", POLYMARKET_WALLET_ID,
            )
            val byOwnerBefore = getWireMockRequestCountByQueryParam(
                "GET", POLYMARKET_WALLET_PATH, "ownerAddress", POLYMARKET_OWNER_ADDRESS,
            )

            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Assert the status is requested by wallet id while the owner address is not derived") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    assertTrue(
                        getWireMockRequestCountByQueryParam(
                            "GET", POLYMARKET_WALLET_PATH, "walletId", POLYMARKET_WALLET_ID,
                        ) > byWalletIdBefore,
                    )
                }
            }
            step("Assert the status was not requested by owner address yet") {
                assertEquals(
                    byOwnerBefore,
                    getWireMockRequestCountByQueryParam(
                        "GET", POLYMARKET_WALLET_PATH, "ownerAddress", POLYMARKET_OWNER_ADDRESS,
                    ),
                )
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed for the 'NOT_CREATED' status") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert the status is requested by owner address once it is derived") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    assertTrue(
                        getWireMockRequestCountByQueryParam(
                            "GET", POLYMARKET_WALLET_PATH, "ownerAddress", POLYMARKET_OWNER_ADDRESS,
                        ) > byOwnerBefore,
                    )
                }
            }
        }
    }

    @Test
    @AllureId("12910")
    @DisplayName("Polymarket: onboarding. Deposit wallet is deployed with the wallet's own addresses")
    fun deployDepositWalletTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)

            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request carries the owner, wallet id and deposit wallet addresses") {
                assertPolymarketDeployRequestMatchesWallet(deployRequestsBefore)
            }
        }
    }

    @Test
    @AllureId("12913")
    @DisplayName("Polymarket: onboarding. L2 credentials are created when they cannot be derived")
    fun l2CredentialsCreatedTest() {
        val credentialsState = "NotExisting"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val deriveRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH)
            val createRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_CREATE_KEY_PATH)

            step("Set WireMock scenario '$POLYMARKET_CREDENTIALS_SCENARIO' to '$credentialsState'") {
                setWireMockScenarioState(POLYMARKET_CREDENTIALS_SCENARIO, credentialsState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request was sent before the credentials were derived") {
                assertPolymarketRequestOrder(
                    firstMethod = "POST",
                    firstPath = POLYMARKET_DEPLOY_PATH,
                    firstRequestsBefore = deployRequestsBefore,
                    secondMethod = "GET",
                    secondPath = POLYMARKET_DERIVE_KEY_PATH,
                    secondRequestsBefore = deriveRequestsBefore,
                )
            }
            step("Assert credentials are derived first, then created") {
                assertTrue(getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH) > deriveRequestsBefore)
                assertEquals(
                    createRequestsBefore + 1,
                    getWireMockRequestCount("POST", POLYMARKET_CREATE_KEY_PATH),
                )
                assertPolymarketRequestOrder(
                    firstMethod = "GET",
                    firstPath = POLYMARKET_DERIVE_KEY_PATH,
                    firstRequestsBefore = deriveRequestsBefore,
                    secondMethod = "POST",
                    secondPath = POLYMARKET_CREATE_KEY_PATH,
                    secondRequestsBefore = createRequestsBefore,
                )
            }
            step("Assert the derive request is signed with the L1 signature") {
                assertPolymarketL1HeadersSent("GET", POLYMARKET_DERIVE_KEY_PATH, deriveRequestsBefore)
            }
            step("Assert the create request is signed with the L1 signature") {
                assertPolymarketL1HeadersSent("POST", POLYMARKET_CREATE_KEY_PATH, createRequestsBefore)
            }
            step("Assert the credentials from the create response are used for the account requests") {
                flakySafely { assertPolymarketL2CredentialsSent(POLYMARKET_CREATED_API_KEY) }
            }
        }
    }

    @Test
    @AllureId("12904")
    @DisplayName("Polymarket: onboarding. Reconnect without L2 credentials, then straight to the account with them")
    fun l2CredentialsPresenceTest() {
        val walletState = "ReadyToTrade"
        val storiesState = "Available"
        val expiredBalanceState = "Expired"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)
            val createRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_CREATE_KEY_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Set WireMock scenario '$POLYMARKET_STORIES_SCENARIO' to '$storiesState'") {
                setWireMockScenarioState(POLYMARKET_STORIES_SCENARIO, storiesState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Reconnect' screen is displayed while there are no L2 credentials") {
                flakySafely { onPolymarketOnboardingScreen { reconnectScreen.assertIsDisplayed() } }
            }
            step("Click on 'Continue predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert 'Stories' screen is not displayed for the existing account") {
                onSwapStoriesScreen { container.assertDoesNotExist() }
            }
            step("Assert the credentials were derived and not created") {
                assertEquals(createRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_CREATE_KEY_PATH))
            }
            step("Go back to 'Main' screen") {
                device.uiDevice.pressBack()
            }
            val deriveRequestsAfterReconnect = getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH)
            val balanceRequestsAfterReconnect = getWireMockRequestCount("GET", POLYMARKET_BALANCE_PATH)
            step("Open Polymarket from the 'Predictions' banner again") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert 'Polymarket main' screen is displayed without onboarding") {
                awaitPolymarketMainScreen()
            }
            step("Assert onboarding 'Welcome' screen is not displayed") {
                onPolymarketOnboardingScreen { welcomeScreen.assertDoesNotExist() }
            }
            step("Assert onboarding 'Reconnect' screen is not displayed") {
                onPolymarketOnboardingScreen { reconnectScreen.assertDoesNotExist() }
            }
            step("Assert 'Stories' screen is not displayed") {
                onSwapStoriesScreen { container.assertDoesNotExist() }
            }
            step("Assert the stored credentials are used for the account requests") {
                flakySafely {
                    assertPolymarketL2CredentialsSent(POLYMARKET_DERIVED_API_KEY, balanceRequestsAfterReconnect)
                }
            }
            step("Assert no signature, deploy or approvals were requested the second time") {
                assertEquals(deriveRequestsAfterReconnect, getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH))
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
                assertEquals(approvalsRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Go back to 'Main' screen after the second open") {
                device.uiDevice.pressBack()
            }
            step("Set WireMock scenario '$POLYMARKET_BALANCE_SCENARIO' to '$expiredBalanceState'") {
                setWireMockScenarioState(POLYMARKET_BALANCE_SCENARIO, expiredBalanceState)
            }
            step("Open Polymarket from the 'Predictions' banner with the expired credentials") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Reconnect' screen is displayed once the credentials are rejected") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPolymarketOnboardingScreen { reconnectScreen.assertIsDisplayed() }
                }
            }
        }
    }

    @Test
    @AllureId("13353")
    @DisplayName("Polymarket: onboarding. Deposit wallet already deployed, the deploy request is not sent")
    fun deployedStatusSkipsDeployTest() {
        val walletState = "Deployed"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            step("Click on the onboarding action button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request was not sent") {
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
            }
            step("Assert the approvals request was sent") {
                assertEquals(approvalsRequestsBefore + 1, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Assert the approvals request carries the owner and deposit wallet addresses") {
                assertPolymarketApprovalsRequestMatchesWallet(approvalsRequestsBefore)
            }
        }
    }

    @Test
    @AllureId("12907")
    @DisplayName("Polymarket: onboarding. Deposit wallet address, nonce, L1 signature and approvals batch")
    fun onboardingKeysAndSignaturesTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val nonceRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_RELAYER_NONCE_PATH)
            val deriveRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            val signedNotBeforeSeconds = System.currentTimeMillis() / MILLIS_IN_SECOND
            step("Click on 'Start predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            val signedNotAfterSeconds = System.currentTimeMillis() / MILLIS_IN_SECOND
            step("Assert the deploy request carries the owner and the deposit wallet derived from it") {
                assertPolymarketDeployRequestMatchesWallet(deployRequestsBefore)
            }
            step("Assert the nonce was requested from the relayer for the owner address") {
                assertPolymarketRelayerNonceRequested(nonceRequestsBefore)
            }
            step("Assert the L1 signature is sent with the credentials request") {
                assertTrue(getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH) > deriveRequestsBefore)
                assertPolymarketL1HeadersSent("GET", POLYMARKET_DERIVE_KEY_PATH, deriveRequestsBefore)
            }
            step("Assert the approvals request was sent once") {
                assertEquals(approvalsRequestsBefore + 1, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Assert the approvals request carries the nonce received from the relayer") {
                assertPolymarketApprovalsNonce(approvalsRequestsBefore)
            }
            step("Assert the approvals request carries the deadline of 10 minutes after signing") {
                assertPolymarketApprovalsDeadline(approvalsRequestsBefore, signedNotBeforeSeconds, signedNotAfterSeconds)
            }
            step("Assert the approvals request carries the list of approval calls") {
                assertPolymarketApprovalsCalls(approvalsRequestsBefore)
            }
            step("Assert the approvals request carries the batch signature") {
                assertPolymarketApprovalsSignature(approvalsRequestsBefore)
            }
            step("Assert the nonce was requested before the approvals were sent") {
                assertPolymarketRequestOrder(
                    firstMethod = "GET",
                    firstPath = POLYMARKET_RELAYER_NONCE_PATH,
                    firstRequestsBefore = nonceRequestsBefore,
                    secondMethod = "POST",
                    secondPath = POLYMARKET_APPROVALS_PATH,
                    secondRequestsBefore = approvalsRequestsBefore,
                )
            }
        }
    }

    @Test
    @AllureId("12924")
    @DisplayName("Polymarket: onboarding. Ready to trade account with no L2 credentials gets a shortened onboarding")
    fun readyToTradeShortenedOnboardingTest() {
        val walletState = "ReadyToTrade"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)
            val deriveRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Reconnect' screen is displayed while there are no L2 credentials") {
                flakySafely { onPolymarketOnboardingScreen { reconnectScreen.assertIsDisplayed() } }
            }
            step("Click on 'Continue predicting' button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request was not sent") {
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
            }
            step("Assert the approvals request was not sent") {
                assertEquals(approvalsRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Assert the new L2 credentials were requested with the L1 signature") {
                assertTrue(getWireMockRequestCount("GET", POLYMARKET_DERIVE_KEY_PATH) > deriveRequestsBefore)
                assertPolymarketL1HeadersSent("GET", POLYMARKET_DERIVE_KEY_PATH, deriveRequestsBefore)
            }
            step("Assert the new L2 credentials are used for the account requests") {
                flakySafely { assertPolymarketL2CredentialsSent(POLYMARKET_DERIVED_API_KEY) }
            }
        }
    }

    @Test
    @AllureId("12915")
    @DisplayName("Polymarket: onboarding. Failed approvals are sent again with the signed batch")
    fun approvalsFailedResendsSignedBatchTest() {
        val walletState = "ApprovalsFailed"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            val statusByWalletIdBefore = getWireMockRequestCountByQueryParam(
                "GET", POLYMARKET_WALLET_PATH, "walletId", POLYMARKET_WALLET_ID,
            )
            val signedNotBeforeSeconds = System.currentTimeMillis() / MILLIS_IN_SECOND
            step("Click on the onboarding action button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            val signedNotAfterSeconds = System.currentTimeMillis() / MILLIS_IN_SECOND
            step("Assert the deploy request was not sent") {
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
            }
            step("Assert the status was requested again by wallet id") {
                assertTrue(
                    getWireMockRequestCountByQueryParam(
                        "GET", POLYMARKET_WALLET_PATH, "walletId", POLYMARKET_WALLET_ID,
                    ) > statusByWalletIdBefore,
                )
            }
            step("Assert the approvals request was sent once") {
                assertEquals(approvalsRequestsBefore + 1, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Assert the approvals request carries the owner and deposit wallet addresses") {
                assertPolymarketApprovalsRequestMatchesWallet(approvalsRequestsBefore)
            }
            step("Assert the approvals request carries the nonce received from the relayer") {
                assertPolymarketApprovalsNonce(approvalsRequestsBefore)
            }
            step("Assert the approvals request carries the deadline of 10 minutes after signing") {
                assertPolymarketApprovalsDeadline(approvalsRequestsBefore, signedNotBeforeSeconds, signedNotAfterSeconds)
            }
            step("Assert the approvals request carries the list of approval calls") {
                assertPolymarketApprovalsCalls(approvalsRequestsBefore)
            }
            step("Assert the approvals request carries the batch signature") {
                assertPolymarketApprovalsSignature(approvalsRequestsBefore)
            }
        }
    }

    @Test
    @AllureId("14139")
    @DisplayName("Polymarket: onboarding. Approvals in progress, the approvals request is not sent")
    fun approvalsInProgressPollsStatusTest() {
        val walletState = "ApprovalsInProgress1"
        val minimumStatusReads = 4

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            val statusRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH)
            step("Click on the onboarding action button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed once the approvals are final") {
                awaitPolymarketMainScreen()
            }
            step("Assert the approvals request was not sent") {
                assertEquals(approvalsRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
            step("Assert the status was read until it became final") {
                assertTrue(
                    getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH) >= statusRequestsBefore + minimumStatusReads,
                )
            }
        }
    }

    @Test
    @AllureId("13354")
    @DisplayName("Polymarket: onboarding. Deployment in progress, the status is polled until it is final")
    fun deploymentInProgressPollsStatusTest() {
        val walletState = "DeploymentInProgress1"
        val minimumStatusPolls = 4

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val statusRequestsBefore = getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            step("Click on the onboarding action button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed once the deployment is final") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request was not sent") {
                assertEquals(deployRequestsBefore, getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH))
            }
            step("Assert the status was polled") {
                assertTrue(
                    getWireMockRequestCount("GET", POLYMARKET_WALLET_PATH) >= statusRequestsBefore + minimumStatusPolls,
                )
            }
            step("Assert the approvals request was sent once the deployment is final") {
                assertEquals(approvalsRequestsBefore + 1, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
        }
    }

    @Test
    @AllureId("13355")
    @DisplayName("Polymarket: onboarding. Failed deployment is deployed again with the wallet's own addresses")
    fun deploymentFailedRedeploysTest() {
        val walletState = "DeploymentFailed"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val deployRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_DEPLOY_PATH)
            val approvalsRequestsBefore = getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH)

            step("Set WireMock scenario '$POLYMARKET_WALLET_SCENARIO' to '$walletState'") {
                setWireMockScenarioState(POLYMARKET_WALLET_SCENARIO, walletState)
            }
            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Open Polymarket from the 'Predictions' banner") {
                openPolymarketFromMarketsBanner()
            }
            step("Assert onboarding 'Welcome' screen is displayed") {
                flakySafely { onPolymarketOnboardingScreen { welcomeScreen.assertIsDisplayed() } }
            }
            step("Click on the onboarding action button") {
                clickPolymarketOnboardingActionButton()
            }
            step("Assert 'Polymarket main' screen is displayed") {
                awaitPolymarketMainScreen()
            }
            step("Assert the deploy request carries the owner, wallet id and deposit wallet addresses") {
                assertPolymarketDeployRequestMatchesWallet(deployRequestsBefore)
            }
            step("Assert the approvals request was sent after the redeploy") {
                assertEquals(approvalsRequestsBefore + 1, getWireMockRequestCount("POST", POLYMARKET_APPROVALS_PATH))
            }
        }
    }

    @Test
    @AllureId("13417")
    @DisplayName("Polymarket: onboarding. Account status is requested for every added wallet")
    fun accountStatusRequestedForAllWalletsTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                System.setProperty(POLYMARKET_AB_TEST_PROPERTY, POLYMARKET_AB_TEST_ON)
            },
            additionalAfterSection = { System.clearProperty(POLYMARKET_AB_TEST_PROPERTY) },
        ).run {
            val walletIdsBefore = findWireMockRequestQueryParamValues("GET", POLYMARKET_WALLET_PATH, "walletId")

            step("Open 'Main' screen with a hot wallet") {
                openMainScreenWithPolymarketHotWallet()
            }
            step("Assert the status is requested by the hot wallet id") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val walletIds = findWireMockRequestQueryParamValues("GET", POLYMARKET_WALLET_PATH, "walletId")
                    assertTrue(walletIds.drop(walletIdsBefore.size).contains(POLYMARKET_WALLET_ID))
                }
            }
            step("Add a card wallet") {
                addNewCardWalletWithoutSync(Wallet2MockContent)
            }
            step("Assert the status is requested by the id of the added wallet as well") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    val walletIds = findWireMockRequestQueryParamValues("GET", POLYMARKET_WALLET_PATH, "walletId")
                        .drop(walletIdsBefore.size)
                    assertTrue(walletIds.any { it != POLYMARKET_WALLET_ID })
                }
            }
        }
    }
}

private const val MILLIS_IN_SECOND = 1_000L