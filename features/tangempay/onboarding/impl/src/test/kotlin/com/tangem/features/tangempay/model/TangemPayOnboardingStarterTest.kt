@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.features.tangempay.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayOnboardingStarterTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val repository: OnboardingRepository = mockk(relaxed = true)
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase = mockk()
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase = mockk()
    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()

    private val loadingChanges = mutableListOf<Boolean>()
    private var exitCount = 0
    private var requestedWalletsIds: List<UserWalletId>? = null
    private val approvedCustomers = mutableListOf<UserWalletId>()

    private val callbacks = TangemPayOnboardingStarter.Callbacks(
        onLoadingChange = { isLoading -> loadingChanges += isLoading },
        onExit = { exitCount++ },
        onWalletSelectorRequested = { walletsIds -> requestedWalletsIds = walletsIds },
        onCustomerAlreadyApproved = { userWalletId -> approvedCustomers += userWalletId },
    )

    private val callbacksWithoutApprovedHandler = TangemPayOnboardingStarter.Callbacks(
        onLoadingChange = { isLoading -> loadingChanges += isLoading },
        onExit = { exitCount++ },
    )

    private val starter = TangemPayOnboardingStarter(
        router = router,
        uiMessageSender = uiMessageSender,
        repository = repository,
        produceInitialDataUseCase = produceInitialDataUseCase,
        getCustomerOffersUseCase = getCustomerOffersUseCase,
        createHotWalletUseCase = createHotWalletUseCase,
        isHotWalletCreationSupported = isHotWalletCreationSupported,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            repository,
            produceInitialDataUseCase,
            getCustomerOffersUseCase,
            createHotWalletUseCase,
            isHotWalletCreationSupported,
            tangemPayFeatureToggles,
        )
        loadingChanges.clear()
        exitCount = 0
        requestedWalletsIds = null
        approvedCustomers.clear()
        every { tangemPayFeatureToggles.isResidencyStepEnabled } returns false
        coEvery { produceInitialDataUseCase(USER_WALLET_ID) } returns Unit.right()
        coEvery { repository.getCustomerInfo(USER_WALLET_ID) } returns customerInfo(KycStatus.PENDING).right()
    }

    @Nested
    inner class CampaignOrder {

        @Test
        fun `GIVEN campaign offer WHEN startOnboarding THEN campaign order created AND KYC opened`() =
            runTest {
                // Arrange
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY)
                } returns campaignOffer().right()
                coEvery {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                } returns ORDER_ID.right()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                coVerify(exactly = 1) {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                }
                verify(exactly = 1) {
                    router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(USER_WALLET_ID), onComplete = any())
                }
            }

        @Test
        fun `GIVEN no campaign offer WHEN startOnboarding THEN dialog shown AND no order AND KYC not opened`() =
            runTest {
                // Arrange
                coEvery { getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY) } returns null.right()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
                coVerify(exactly = 0) { repository.createOrder(any(), any(), any(), any()) }
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
                assertThat(loadingChanges).containsExactly(true, false).inOrder()
            }

        @Test
        fun `GIVEN no campaign offer WHEN dialog dismissed THEN exit callback invoked`() = runTest {
            // Arrange
            coEvery { getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY) } returns null.right()
            val messages = mutableListOf<DialogMessage>()
            every { uiMessageSender.send(capture(messages)) } returns Unit

            // Act
            starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)
            messages.single().onDismissRequest()

            // Assert
            assertThat(exitCount).isEqualTo(1)
        }

        @Test
        fun `GIVEN offers request fails WHEN startOnboarding THEN snackbar shown AND KYC not opened`() =
            runTest {
                // Arrange
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY)
                } returns VisaApiError.Unspecified.left()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
                coVerify(exactly = 0) { repository.createOrder(any(), any(), any(), any()) }
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
                assertThat(loadingChanges).containsExactly(true, false).inOrder()
            }

        @Test
        fun `GIVEN order creation fails WHEN startOnboarding THEN snackbar shown AND KYC not opened`() =
            runTest {
                // Arrange
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY)
                } returns campaignOffer().right()
                coEvery {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                } returns VisaApiError.Unspecified.left()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
                assertThat(loadingChanges).containsExactly(true, false).inOrder()
            }

        @Test
        fun `GIVEN campaign offer WHEN checkCustomerInfo continues to KYC THEN campaign order created first`() =
            runTest {
                // Arrange
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY)
                } returns campaignOffer().right()
                coEvery {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                } returns ORDER_ID.right()

                // Act
                starter.checkCustomerInfo(
                    userWalletId = USER_WALLET_ID,
                    mode = TangemPayOnboardingStarter.Mode.ContinueToKyc,
                    campaignKey = CAMPAIGN_KEY,
                    callbacks = callbacks,
                )

                // Assert
                coVerify(exactly = 1) {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                }
                verify(exactly = 1) {
                    router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(USER_WALLET_ID), onComplete = any())
                }
            }

    }

    @Nested
    inner class ApprovedCustomer {

        @Test
        fun `GIVEN approved customer and a handler WHEN checkCustomerInfo THEN handler invoked instead of exit`() =
            runTest {
                // Arrange
                givenApprovedCustomer()

                // Act
                starter.checkCustomerInfo(
                    userWalletId = USER_WALLET_ID,
                    mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                    campaignKey = CAMPAIGN_KEY,
                    callbacks = callbacks,
                )

                // Assert
                assertThat(approvedCustomers).containsExactly(USER_WALLET_ID)
                assertThat(exitCount).isEqualTo(0)
                coVerify(exactly = 0) { produceInitialDataUseCase(any()) }
                coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
                coVerify(exactly = 0) { repository.createOrder(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN approved customer and a handler WHEN startOnboarding THEN handler invoked after re-auth`() =
            runTest {
                // Arrange
                givenApprovedCustomer()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                assertThat(approvedCustomers).containsExactly(USER_WALLET_ID)
                assertThat(exitCount).isEqualTo(0)
                coVerify(exactly = 1) { produceInitialDataUseCase(USER_WALLET_ID) }
                coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
                coVerify(exactly = 0) { repository.createOrder(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN approved customer and no handler WHEN checkCustomerInfo THEN exit`() = runTest {
            // Arrange
            givenApprovedCustomer()

            // Act
            starter.checkCustomerInfo(
                userWalletId = USER_WALLET_ID,
                mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                campaignKey = null,
                callbacks = callbacksWithoutApprovedHandler,
            )

            // Assert
            assertThat(exitCount).isEqualTo(1)
            assertThat(approvedCustomers).isEmpty()
        }

        @Test
        fun `GIVEN approved customer and no handler WHEN startOnboarding THEN exit`() = runTest {
            // Arrange
            givenApprovedCustomer()

            // Act
            starter.startOnboarding(USER_WALLET_ID, campaignKey = null, callbacksWithoutApprovedHandler)

            // Assert
            assertThat(exitCount).isEqualTo(1)
            assertThat(approvedCustomers).isEmpty()
        }

        private fun givenApprovedCustomer() {
            coEvery { repository.getCustomerInfo(USER_WALLET_ID) } returns customerInfo(KycStatus.APPROVED).right()
        }
    }

    @Nested
    inner class RegularOnboarding {

        @Test
        fun `GIVEN no campaign WHEN startOnboarding THEN offers not requested AND key not cleared AND KYC opened`() =
            runTest {
                // Act
                starter.startOnboarding(USER_WALLET_ID, campaignKey = null, callbacks)

                // Assert
                coVerify(exactly = 0) { getCustomerOffersUseCase.campaignOffer(any(), any()) }
                coVerify(exactly = 0) { repository.createOrder(any(), any(), any(), any()) }
                verify(exactly = 1) {
                    router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(USER_WALLET_ID), onComplete = any())
                }
            }

        @Test
        fun `GIVEN initial data fails WHEN startOnboarding THEN snackbar shown AND loading reset AND KYC not opened`() =
            runTest {
                // Arrange
                coEvery { produceInitialDataUseCase(USER_WALLET_ID) } returns RuntimeException("boom").left()

                // Act
                starter.startOnboarding(USER_WALLET_ID, campaignKey = null, callbacks)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
                assertThat(loadingChanges).containsExactly(true, false).inOrder()
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
            }

        @Test
        fun `GIVEN customer info fails after re-auth WHEN startOnboarding THEN snackbar shown AND KYC not opened`() =
            runTest {
                // Arrange
                coEvery { repository.getCustomerInfo(USER_WALLET_ID) } returns VisaApiError.ServerUnavailable.left()

                // Act
                starter.startOnboarding(USER_WALLET_ID, campaignKey = null, callbacks)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
                assertThat(loadingChanges).containsExactly(true, false).inOrder()
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
            }
    }

    @Nested
    inner class ProceedWithWallets {

        @Test
        fun `GIVEN no wallets WHEN proceedWithWallets THEN exits AND wallet selector not requested`() = runTest {
            // Act
            starter.proceedWithWallets(
                walletsIds = emptyList(),
                mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                campaignKey = null,
                callbacks = callbacks,
            )

            // Assert
            assertThat(exitCount).isEqualTo(1)
            assertThat(requestedWalletsIds).isNull()
            coVerify(exactly = 0) { repository.getCustomerInfo(any()) }
        }

        @Test
        fun `GIVEN single wallet WHEN proceedWithWallets THEN customer info checked for it`() = runTest {
            // Act
            starter.proceedWithWallets(
                walletsIds = listOf(USER_WALLET_ID),
                mode = TangemPayOnboardingStarter.Mode.ContinueToKyc,
                campaignKey = null,
                callbacks = callbacks,
            )

            // Assert
            coVerify(exactly = 1) { repository.getCustomerInfo(USER_WALLET_ID) }
            assertThat(requestedWalletsIds).isNull()
        }

        @Test
        fun `GIVEN several wallets WHEN proceedWithWallets THEN wallet selector requested`() = runTest {
            // Arrange
            val walletsIds = listOf(USER_WALLET_ID, UserWalletId("022"))

            // Act
            starter.proceedWithWallets(
                walletsIds = walletsIds,
                mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                campaignKey = CAMPAIGN_KEY,
                callbacks = callbacks,
            )

            // Assert
            assertThat(requestedWalletsIds).isEqualTo(walletsIds)
            coVerify(exactly = 0) { repository.getCustomerInfo(any()) }
        }
    }

    @Nested
    inner class CreateHotWallet {

        @Test
        fun `GIVEN creation not supported WHEN createHotWallet THEN home opened AND wallet not created`() = runTest {
            // Arrange
            every { isHotWalletCreationSupported() } returns false
            every { isHotWalletCreationSupported.getLeastVersionName() } returns "Android 10"

            // Act
            val actual = starter.createHotWallet(onLoadingChange = {})

            // Assert
            assertThat(actual).isNull()
            verify(exactly = 1) { router.replaceCurrent(AppRoute.Home()) }
            coVerify(exactly = 0) { createHotWalletUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN creation succeeds WHEN createHotWallet THEN wallet returned`() = runTest {
            // Arrange
            val userWallet: UserWallet.Hot = mockk()
            every { isHotWalletCreationSupported() } returns true
            coEvery { createHotWalletUseCase(any(), any()) } returns userWallet.right()

            // Act
            val actual = starter.createHotWallet(onLoadingChange = {})

            // Assert
            assertThat(actual).isSameInstanceAs(userWallet)
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN creation fails WHEN createHotWallet THEN dialog shown AND loading reset AND null returned`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns true
                coEvery { createHotWalletUseCase(any(), any()) } returns RuntimeException("boom").left()

                // Act
                val actual = starter.createHotWallet(onLoadingChange = callbacks.onLoadingChange)

                // Assert
                assertThat(actual).isNull()
                assertThat(loadingChanges).containsExactly(false)
                verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            }
    }

    @Nested
    inner class ResidencyStep {

        @Test
        fun `GIVEN residency step enabled WHEN startOnboarding THEN residency opened instead of KYC`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true

            // Act
            starter.startOnboarding(USER_WALLET_ID, campaignKey = null, callbacks)

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    AppRoute.Wallet,
                    AppRoute.TangemPayEligibility(
                        product = TangemPayProduct.PAYMENT_ACCOUNT,
                        userWalletId = USER_WALLET_ID,
                    ),
                    onComplete = any(),
                )
            }
            verify(exactly = 0) { router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(USER_WALLET_ID), onComplete = any()) }
        }

        @Test
        fun `GIVEN residency step enabled and campaign WHEN startOnboarding THEN order created before residency`() =
            runTest {
                // Arrange
                every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(USER_WALLET_ID, CAMPAIGN_KEY)
                } returns campaignOffer().right()
                coEvery {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                } returns ORDER_ID.right()

                // Act
                starter.startOnboarding(USER_WALLET_ID, CAMPAIGN_KEY, callbacks)

                // Assert
                coVerify(exactly = 1) {
                    repository.createOrder(USER_WALLET_ID, CAMPAIGN_KEY, SPECIFICATION_NAME, ORDER_TYPE)
                }
                verify(exactly = 1) {
                    router.replaceAll(
                        AppRoute.Wallet,
                        AppRoute.TangemPayEligibility(
                            product = TangemPayProduct.PAYMENT_ACCOUNT,
                            userWalletId = USER_WALLET_ID,
                        ),
                        onComplete = any(),
                    )
                }
            }

        @Test
        fun `GIVEN residency step enabled WHEN openKyc THEN KYC opened directly`() = runTest {
            // Arrange
            every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true

            // Act
            starter.openKyc(USER_WALLET_ID)

            // Assert
            verify(exactly = 1) {
                router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(USER_WALLET_ID), onComplete = any())
            }
        }
    }

    private fun customerInfo(kycStatus: KycStatus) = CustomerInfo(
        customerId = "cust_1",
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = kycStatus,
        state = CustomerInfo.State.NEW,
        fiatBalance = null,
        tariffPlan = null,
    )

    private fun campaignOffer() = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
        fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = SPECIFICATION_NAME, orderType = ORDER_TYPE),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val ORDER_TYPE = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val SPECIFICATION_NAME = "SP_000009"
        const val ORDER_ID = "order_1"
    }
}