@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.features.tangempay.campaign

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.features.tangempay.model.TangemPayOnboardingStarter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayCampaignModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val campaignRepository: TangemPayCampaignRepository = mockk()
    private val eligibilityManager: TangemPayEligibilityManager = mockk()
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase = mockk()
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val onboardingStarter: TangemPayOnboardingStarter = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            urlOpener,
            userWalletsListRepository,
            onboardingRepository,
            campaignRepository,
            eligibilityManager,
            getCustomerOffersUseCase,
            clearAppsFlyerDeeplinkUseCase,
            onboardingStarter,
        )
        coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_DEEPLINK) } returns true.right()
        coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns CAMPAIGN.right()
        coEvery { eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false) } returns emptyList()
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
    }

    @Nested
    inner class Resolution {

        @Test
        fun `GIVEN valid campaign and no wallets WHEN model created THEN landing in NoWallet mode`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val landing = model.uiState.value as TangemPayCampaignUM.Landing
            assertThat(landing.mode).isEqualTo(TangemPayCampaignUM.Landing.Mode.NoWallet)
            assertThat(landing.cardArtUrl).isEqualTo(CARD_ART_URL)
            assertThat(landing.isCtaLoading).isFalse()
            coVerify(exactly = 1) { onboardingRepository.validateDeeplink(CAMPAIGN_DEEPLINK) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN wallets without Pay WHEN model created THEN landing in NoPay mode`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns false.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val landing = model.uiState.value as TangemPayCampaignUM.Landing
            assertThat(landing.mode).isEqualTo(TangemPayCampaignUM.Landing.Mode.NoPay)
            model.onDestroy()
        }

        @Test
        fun `GIVEN wallet with Pay WHEN model created THEN offers requested with deeplink AND landing not shown`() =
            runTest {
                // Arrange
                givenWallets(WALLET_ID)
                coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns true.right()
                coEvery {
                    getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY)
                } returns campaignOffer().right()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isEqualTo(TangemPayCampaignUM.Loading)
                coVerify(exactly = 1) { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) }
                verify(exactly = 0) { uiMessageSender.send(any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN wallet with Pay and no campaign offer WHEN model created THEN terminal dialog shown`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns true.right()
            coEvery { getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY) } returns null.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            assertThat(model.uiState.value).isEqualTo(TangemPayCampaignUM.Loading)
            model.onDestroy()
        }

        @Test
        fun `GIVEN validation false WHEN model created THEN terminal dialog shown AND dismiss closes container`() =
            runTest {
                // Arrange
                coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_DEEPLINK) } returns false.right()
                val messages = mutableListOf<DialogMessage>()
                every { uiMessageSender.send(capture(messages)) } returns Unit

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()
                messages.single().onDismissRequest()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) { campaignRepository.getCampaign(any()) }
                coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
                verify(exactly = 1) { router.replaceAll(AppRoute.Home()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN validation error WHEN model created THEN error state`() = runTest {
            // Arrange
            coEvery { onboardingRepository.validateDeeplink(CAMPAIGN_DEEPLINK) } returns UNIVERSAL_ERROR.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN campaign request error WHEN model created THEN error state`() = runTest {
            // Arrange
            coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns VisaApiError.Unspecified.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN Pay check error WHEN model created THEN error state`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns VisaApiError.Unspecified.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN offers error in Pay branch WHEN model created THEN error state`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns true.right()
            coEvery {
                getCustomerOffersUseCase.campaignOffer(WALLET_ID, CAMPAIGN_KEY)
            } returns VisaApiError.Unspecified.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN error state WHEN retry THEN campaign resolved again`() = runTest {
            // Arrange
            coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returnsMany listOf(
                VisaApiError.Unspecified.left(),
                CAMPAIGN.right(),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onRetry()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Landing::class.java)
            coVerify(exactly = 2) { campaignRepository.getCampaign(CAMPAIGN_KEY) }
            model.onDestroy()
        }
    }

    @Nested
    inner class Exit {

        @Test
        fun `GIVEN no wallets WHEN onClose THEN key cleared AND replaces all with Home`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            verify { router.replaceAll(AppRoute.Home()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN wallets present WHEN onClose THEN key cleared AND replaces all with Wallet`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns false.right()
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            verify { router.replaceAll(AppRoute.Wallet) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN error state WHEN close clicked THEN key cleared AND container closed`() = runTest {
            // Arrange
            coEvery { campaignRepository.getCampaign(CAMPAIGN_KEY) } returns VisaApiError.Unspecified.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onClose()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            verify { router.replaceAll(AppRoute.Home()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing in NoPay mode WHEN close clicked THEN key cleared AND replaces all with Wallet`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns false.right()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onCloseClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            verify { router.replaceAll(AppRoute.Wallet) }
            model.onDestroy()
        }
    }

    @Nested
    inner class LandingActions {

        @Test
        fun `GIVEN NoWallet landing WHEN get card THEN hot wallet created AND onboarding started with campaign`() =
            runTest {
                // Arrange
                val userWallet: UserWallet.Hot = mockk { every { walletId } returns WALLET_ID }
                coEvery { onboardingStarter.createHotWallet(CAMPAIGN_KEY, any()) } returns userWallet
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                (model.uiState.value as TangemPayCampaignUM.Landing).onGetCardClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onboardingStarter.createHotWallet(CAMPAIGN_KEY, any()) }
                coVerify(exactly = 1) { onboardingStarter.startOnboarding(WALLET_ID, CAMPAIGN_KEY, any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN NoWallet landing WHEN hot wallet not created THEN onboarding not started`() = runTest {
            // Arrange
            coEvery { onboardingStarter.createHotWallet(CAMPAIGN_KEY, any()) } returns null
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onGetCardClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { onboardingStarter.startOnboarding(any(), any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN NoPay landing WHEN get card THEN possible wallets passed to starter with campaign`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            coEvery { onboardingRepository.hasTangemPayInWallet(WALLET_ID) } returns false.right()
            coEvery {
                eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = true)
            } returns listOf(WALLET_ID)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onGetCardClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                onboardingStarter.proceedWithWallets(
                    walletsIds = listOf(WALLET_ID),
                    mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                    campaignKey = CAMPAIGN_KEY,
                    callbacks = any(),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing WHEN terms clicked THEN terms link opened`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onTermsClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith(TangemPayConstants.TERMS_AND_LIMITS_URL) }) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing WHEN wallet selected THEN customer info checked with campaign`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onWalletSelected(WALLET_ID)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                onboardingStarter.checkCustomerInfo(
                    userWalletId = WALLET_ID,
                    mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                    campaignKey = CAMPAIGN_KEY,
                    callbacks = any(),
                )
            }
            model.onDestroy()
        }
    }

    private fun givenWallets(vararg walletsIds: UserWalletId) {
        coEvery { userWalletsListRepository.userWalletsSync() } returns walletsIds.map { mockk<UserWallet>() }
        coEvery {
            eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false)
        } returns walletsIds.toList()
    }

    private fun campaignOffer() = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
        fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "SP_000009", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2),
    )

    private fun createModel(testScope: TestScope): TangemPayCampaignModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return TangemPayCampaignModel(
            paramsContainer = MutableParamsContainer(
                TangemPayCampaignComponent.Params(campaignKey = CAMPAIGN_KEY),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            router = router,
            uiMessageSender = uiMessageSender,
            urlOpener = urlOpener,
            userWalletsListRepository = userWalletsListRepository,
            onboardingRepository = onboardingRepository,
            campaignRepository = campaignRepository,
            eligibilityManager = eligibilityManager,
            getCustomerOffersUseCase = getCustomerOffersUseCase,
            clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
            onboardingStarter = onboardingStarter,
        )
    }

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CAMPAIGN_DEEPLINK = "tangem://marketing/tpay_for_oxford"
        const val CARD_ART_URL = "https://example.com/oxford.png"
        val WALLET_ID = UserWalletId("011")
        val CAMPAIGN = TangemPayCampaign(cardArtUrl = CARD_ART_URL)
        val UNIVERSAL_ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}