@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.features.tangempay.campaign

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
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
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.ResolveTangemPayCampaignUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.features.tangempay.model.TangemPayOnboardingStarter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
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
    private val eligibilityManager: TangemPayEligibilityManager = mockk()
    private val resolveTangemPayCampaignUseCase: ResolveTangemPayCampaignUseCase = mockk()
    private val produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase = mockk()
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val onboardingStarter: TangemPayOnboardingStarter = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            urlOpener,
            userWalletsListRepository,
            eligibilityManager,
            resolveTangemPayCampaignUseCase,
            produceTangemPayInitialDataUseCase,
            clearAppsFlyerDeeplinkUseCase,
            onboardingStarter,
            analytics,
        )
        givenResolution(TangemPayCampaignResolution.Landing(CAMPAIGN))
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        coEvery { produceTangemPayInitialDataUseCase(any()) } returns Unit.right()
    }

    @Nested
    inner class Resolution {

        @Test
        fun `GIVEN landing resolution and no wallets WHEN model created THEN landing in NoWallet mode`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val landing = model.uiState.value as TangemPayCampaignUM.Landing
            assertThat(landing.mode).isEqualTo(TangemPayCampaignUM.Landing.Mode.NoWallet)
            assertThat(landing.cardArtUrl).isEqualTo(CARD_ART_URL)
            assertThat(landing.isCtaLoading).isFalse()
            coVerify(exactly = 1) { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing resolution without art WHEN model created THEN landing shown with no art`() = runTest {
            // Arrange
            givenResolution(TangemPayCampaignResolution.Landing(TangemPayCampaign(cardArtUrl = null)))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val landing = model.uiState.value as TangemPayCampaignUM.Landing
            assertThat(landing.cardArtUrl).isNull()
            assertThat(landing.mode).isEqualTo(TangemPayCampaignUM.Landing.Mode.NoWallet)
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing resolution and wallets present WHEN model created THEN landing in NoPay mode`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val landing = model.uiState.value as TangemPayCampaignUM.Landing
            assertThat(landing.mode).isEqualTo(TangemPayCampaignUM.Landing.Mode.NoPay)
            model.onDestroy()
        }

        @Test
        fun `GIVEN issue card resolution WHEN model created THEN child opened with the offer art`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            givenResolution(issueCardResolution(offer = campaignOffer(mainImageUrl = OFFER_ART_URL)))

            // Act
            val model = createModel(testScope = this)
            val children = model.trackChildren()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(TangemPayCampaignUM.Loading)
            assertThat(children).containsExactly(
                TangemPayCampaignChild.IssueCard(
                    userWalletId = WALLET_ID,
                    cardArtUrl = OFFER_ART_URL,
                    feeAmount = BigDecimal.ZERO,
                    feeCurrency = Currency.getInstance("USD"),
                ),
            )
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN offer without art WHEN model created THEN child uses the campaign art`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            givenResolution(issueCardResolution(offer = campaignOffer()))

            // Act
            val model = createModel(testScope = this)
            val children = model.trackChildren()
            advanceUntilIdle()

            // Assert
            assertThat(children.single()?.cardArtUrl).isEqualTo(CARD_ART_URL)
            model.onDestroy()
        }

        @Test
        fun `GIVEN unavailable resolution WHEN model created THEN terminal dialog shown AND dismiss closes`() =
            runTest {
                // Arrange
                givenResolution(TangemPayCampaignResolution.Unavailable)
                val messages = mutableListOf<DialogMessage>()
                every { uiMessageSender.send(capture(messages)) } returns Unit

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()
                messages.single().onDismissRequest()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isEqualTo(TangemPayCampaignUM.Loading)
                verify(exactly = 1) { router.replaceAll(AppRoute.Home()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN resolution error WHEN model created THEN error state AND no dialog`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returns UNIVERSAL_ERROR.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN error state WHEN retry THEN campaign resolved again`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returnsMany listOf(
                UNIVERSAL_ERROR.left(),
                TangemPayCampaignResolution.Landing(CAMPAIGN).right(),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onRetry()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Landing::class.java)
            coVerify(exactly = 2) { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN session expired WHEN model created THEN session renewed AND campaign resolved again`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returnsMany listOf(
                TangemPayCampaignResolution.SessionExpired(WALLET_ID).right(),
                TangemPayCampaignResolution.Landing(CAMPAIGN).right(),
            )

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Landing::class.java)
            coVerify(exactly = 1) { produceTangemPayInitialDataUseCase(WALLET_ID) }
            coVerify(exactly = 2) { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN two expired sessions WHEN model created THEN both renewed AND campaign resolved`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returnsMany listOf(
                TangemPayCampaignResolution.SessionExpired(WALLET_ID).right(),
                TangemPayCampaignResolution.SessionExpired(OTHER_WALLET_ID).right(),
                TangemPayCampaignResolution.Landing(CAMPAIGN).right(),
            )

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Landing::class.java)
            coVerify(exactly = 1) { produceTangemPayInitialDataUseCase(WALLET_ID) }
            coVerify(exactly = 1) { produceTangemPayInitialDataUseCase(OTHER_WALLET_ID) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN session renewal fails WHEN model created THEN error state AND campaign not resolved again`() =
            runTest {
                // Arrange
                givenResolution(TangemPayCampaignResolution.SessionExpired(WALLET_ID))
                coEvery { produceTangemPayInitialDataUseCase(WALLET_ID) } returns IllegalStateException().left()

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
                coVerify(exactly = 1) { produceTangemPayInitialDataUseCase(WALLET_ID) }
                coVerify(exactly = 1) { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN session still expired after renewal WHEN model created THEN error state AND no second renewal`() =
            runTest {
                // Arrange
                givenResolution(TangemPayCampaignResolution.SessionExpired(WALLET_ID))

                // Act
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
                coVerify(exactly = 1) { produceTangemPayInitialDataUseCase(WALLET_ID) }
                coVerify(exactly = 2) { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN renewal exhausted WHEN retry THEN the session is renewed again`() = runTest {
            // Arrange
            givenResolution(TangemPayCampaignResolution.SessionExpired(WALLET_ID))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onRetry()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayCampaignUM.Error::class.java)
            coVerify(exactly = 2) { produceTangemPayInitialDataUseCase(WALLET_ID) }
            model.onDestroy()
        }
    }

    @Nested
    inner class Deeplink {

        @Test
        fun `GIVEN campaign deeplink WHEN model created THEN deeplink cleared`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN resolution fails WHEN model created THEN deeplink still cleared`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returns UNIVERSAL_ERROR.left()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { clearAppsFlyerDeeplinkUseCase() }
            model.onDestroy()
        }
    }

    @Nested
    inner class Exit {

        @Test
        fun `GIVEN no wallets WHEN onClose THEN replaces all with Home`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            verify { router.replaceAll(AppRoute.Home()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN wallets present WHEN onClose THEN replaces all with Wallet`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            val model = createModel(testScope = this)

            // Act
            model.onClose()
            advanceUntilIdle()

            // Assert
            verify { router.replaceAll(AppRoute.Wallet) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN error state WHEN close clicked THEN container closed`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returns UNIVERSAL_ERROR.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onClose()
            advanceUntilIdle()

            // Assert
            verify { router.replaceAll(AppRoute.Home()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing in NoPay mode WHEN close clicked THEN replaces all with Wallet`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onCloseClick()
            advanceUntilIdle()

            // Assert
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
                coEvery { onboardingStarter.createHotWallet(any()) } returns userWallet
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                (model.uiState.value as TangemPayCampaignUM.Landing).onGetCardClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 1) { onboardingStarter.createHotWallet(any()) }
                coVerify(exactly = 1) { onboardingStarter.startOnboarding(WALLET_ID, CAMPAIGN_KEY, any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN NoWallet landing WHEN hot wallet not created THEN onboarding not started`() = runTest {
            // Arrange
            coEvery { onboardingStarter.createHotWallet(any()) } returns null
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
            coEvery {
                eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)
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
        fun `GIVEN NoPay landing WHEN get card AND no eligible wallets THEN container closed AND no onboarding`() =
            runTest {
                // Arrange
                givenWallets(WALLET_ID)
                coEvery {
                    eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)
                } returns emptyList()
                val model = createModel(testScope = this)
                advanceUntilIdle()

                // Act
                (model.uiState.value as TangemPayCampaignUM.Landing).onGetCardClick()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) { onboardingStarter.proceedWithWallets(any(), any(), any(), any()) }
                verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN NoWallet landing WHEN terms of service clicked THEN disclaimer opened`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onTermsOfServiceClick()

            // Assert
            verify(exactly = 1) { router.push(AppRoute.Disclaimer(isTosAccepted = true)) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN NoWallet landing WHEN privacy policy clicked THEN privacy policy link opened`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Landing).onPrivacyPolicyClick()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(match { it.startsWith(TangemPayConstants.PRIVACY_POLICY_URL) }) }
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

    @Nested
    inner class Analytics {

        @Test
        fun `GIVEN landing resolution and no wallets WHEN model created THEN new hot wallet event sent`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CobrandOnboardingNewHotWallet()) }
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN landing resolution and wallets present WHEN model created THEN not in pay event sent`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CobrandOnboardingNotInPay()) }
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN issue card resolution WHEN model created THEN exist pay event sent`() = runTest {
            // Arrange
            givenWallets(WALLET_ID)
            givenResolution(issueCardResolution(offer = campaignOffer()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CobrandOnboardingExistPay()) }
            verify(exactly = 1) { analytics.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN unavailable resolution WHEN model created THEN no event sent`() = runTest {
            // Arrange
            givenResolution(TangemPayCampaignResolution.Unavailable)

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            verify { analytics wasNot Called }
            model.onDestroy()
        }

        @Test
        fun `GIVEN resolution error WHEN retry succeeds THEN event sent once`() = runTest {
            // Arrange
            coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returnsMany listOf(
                UNIVERSAL_ERROR.left(),
                TangemPayCampaignResolution.Landing(CAMPAIGN).right(),
            )
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            (model.uiState.value as TangemPayCampaignUM.Error).onRetry()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CobrandOnboardingNewHotWallet()) }
            model.onDestroy()
        }
    }

    private fun givenResolution(resolution: TangemPayCampaignResolution) {
        coEvery { resolveTangemPayCampaignUseCase(CAMPAIGN_KEY) } returns resolution.right()
    }

    private fun givenWallets(vararg walletsIds: UserWalletId) {
        coEvery { userWalletsListRepository.userWalletsSync() } returns walletsIds.map { mockk<UserWallet>() }
    }

    private fun issueCardResolution(offer: Offer) = TangemPayCampaignResolution.IssueCard(
        userWalletId = WALLET_ID,
        campaign = CAMPAIGN,
        offer = offer,
    )

    private fun campaignOffer(mainImageUrl: String? = null) = Offer(
        type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK,
        fee = Offer.Fee(amount = BigDecimal.ZERO, currency = Currency.getInstance("USD")),
        data = Offer.Data(specificationName = "SP_000009", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2),
        mainImageUrl = mainImageUrl,
    )

    private fun TangemPayCampaignModel.trackChildren(): List<TangemPayCampaignChild.IssueCard?> {
        val children = mutableListOf<TangemPayCampaignChild.IssueCard?>()
        childNavigation.subscribe { event -> children += event.transformer(null) as TangemPayCampaignChild.IssueCard? }
        return children
    }

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
            eligibilityManager = eligibilityManager,
            resolveTangemPayCampaignUseCase = resolveTangemPayCampaignUseCase,
            produceTangemPayInitialDataUseCase = produceTangemPayInitialDataUseCase,
            clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
            onboardingStarter = onboardingStarter,
            analytics = analytics,
        )
    }

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val CARD_ART_URL = "https://example.com/oxford.png"
        const val OFFER_ART_URL = "https://example.com/oxford-offer.png"
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("012")
        val CAMPAIGN = TangemPayCampaign(cardArtUrl = CARD_ART_URL)
        val UNIVERSAL_ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}