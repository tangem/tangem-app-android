package com.tangem.features.promobanners.impl.campaigns.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.EnrolledTokenReward
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward
import com.tangem.domain.promo.usecase.EnrollPromoCampaignUseCase
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.component.ActivateCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ActivateCampaignsModelTest {

    private val chooseTokenBridgeFactory: ChooseTokenBridge.Factory = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val enrollPromoCampaignUseCase: EnrollPromoCampaignUseCase = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getPromoCampaignStateUseCase: GetPromoCampaignStateUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val predefinedTokenResolver: PredefinedTokenResolver = mockk(relaxed = true)
    private val modelCallbacks: ActivateCampaignBottomSheetComponent.ActivateCampaignModelCallbacks =
        mockk(relaxed = true)

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private lateinit var onCurrencyChosen: Channel<ChooseTokenResult>

    @BeforeEach
    fun setup() {
        clearMocks(
            getSelectedAppCurrencyUseCase,
            multiAccountListSupplier,
            enrollPromoCampaignUseCase,
            getWalletsUseCase,
            messageSender,
            analyticsEventHandler,
            modelCallbacks,
        )
    }

    @ParameterizedTest
    @MethodSource("provideCampaignTypes")
    fun `GIVEN campaign type WHEN model created THEN PromotionScreenOpened is sent`(campaignType: CampaignType) =
        runTest {
            // Act
            val model = createModel(campaignType)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaignType))
            }
            model.onDestroy()
        }

    @Test
    fun `WHEN choose token clicked and dismissed THEN isChoosingToken toggles`() = runTest {
        // Arrange
        val model = createModel(CampaignType.WhaleSwapCashback(campaignId = "1"))
        advanceUntilIdle()

        // Act & Assert
        model.uiState.value.onChooseTokenClick()
        assertThat(model.uiState.value.isChoosingToken).isTrue()

        model.uiState.value.onChooseTokenDismiss()
        assertThat(model.uiState.value.isChoosingToken).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a coin is chosen WHEN currency chosen THEN it is ignored`() = runTest {
        // Arrange
        val model = createModel(CampaignType.WhaleSwapCashback(campaignId = "1"))
        advanceUntilIdle()

        // Act
        onCurrencyChosen.send(chooseTokenResult(currency = coin()))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.selectedToken).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a token chosen and enroll succeeds WHEN enroll clicked THEN event carries symbol and blockchain`() =
        runTest {
            // Arrange
            val token = token()
            val campaignType = CampaignType.WhaleSwapCashback(campaignId = "1")
            coEvery { enrollPromoCampaignUseCase.invoke(any(), any(), any()) } returns
                Either.Right(EnrollResult.Success(EnrolledTokenReward(tokenAddress = "a", networkId = "b", tokenId = "d")))
            val model = createModel(campaignType)
            advanceUntilIdle()

            // Act
            onCurrencyChosen.send(chooseTokenResult(currency = token))
            advanceUntilIdle()
            model.uiState.value.footerUM.onPrimaryButtonClick()
            advanceUntilIdle()

            // Assert — Token param is the symbol (TTK), Blockchain param is the network name (Ethereum)
            val events = mutableListOf<AnalyticsEvent>()
            verify { analyticsEventHandler.send(capture(events)) }
            val enrollEvent = events.filterIsInstance<PromoCampaignsAnalyticsEvent.EnrollButtonClicked>().single()
            assertThat(enrollEvent.event).isEqualTo("Enroll Button Clicked")
            assertThat(enrollEvent.params).containsExactly(
                "Campaign", "Cashback",
                "Token", "TTK",
                "Blockchain", "Ethereum",
            )

            coVerify(exactly = 1) {
                enrollPromoCampaignUseCase.invoke(
                    campaign = PromoCampaignId.WhaleSwapCashback,
                    tokenReward = TokenReward(
                        tokenAddress = token.contractAddress,
                        networkId = token.network.rawId,
                        userAddress = userAddress,
                        tokenId = token.id.rawCurrencyId?.value.orEmpty(),
                    ),
                    walletIds = allWalletIds,
                )
            }
            verify(exactly = 1) { modelCallbacks.onActivated(campaignType) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN enroll in progress WHEN enroll clicked again THEN use case invoked once`() = runTest {
        // Arrange
        coEvery { enrollPromoCampaignUseCase.invoke(any(), any(), any()) } returns
            Either.Right(EnrollResult.Success(EnrolledTokenReward(tokenAddress = "a", networkId = "b", tokenId = "d")))
        val model = createModel(CampaignType.WhaleSwapCashback(campaignId = "1"))
        advanceUntilIdle()
        onCurrencyChosen.send(chooseTokenResult(currency = token()))
        advanceUntilIdle()

        // Act — click twice before the in-flight enroll coroutine gets a chance to run
        model.uiState.value.footerUM.onPrimaryButtonClick()
        model.uiState.value.footerUM.onPrimaryButtonClick()
        advanceUntilIdle()

        // Assert — the re-entrant click is ignored: enroll is triggered only once
        coVerify(exactly = 1) { enrollPromoCampaignUseCase.invoke(any(), any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN enroll returns AlreadyEnrolled WHEN enroll clicked THEN onAlreadyActivated called`() = runTest {
        // Arrange
        val campaignType = CampaignType.WhaleSwapCashback(campaignId = "1")
        coEvery { enrollPromoCampaignUseCase.invoke(any(), any(), any()) } returns
            Either.Right(EnrollResult.AlreadyEnrolled(EnrolledTokenReward(tokenAddress = "a", networkId = "b", tokenId = "d")))
        val model = createModel(campaignType)
        advanceUntilIdle()

        // Act
        onCurrencyChosen.send(chooseTokenResult(currency = token()))
        advanceUntilIdle()
        model.uiState.value.footerUM.onPrimaryButtonClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { modelCallbacks.onAlreadyActivated(campaignType) }
        verify(exactly = 0) { modelCallbacks.onActivated(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN enroll fails WHEN enroll clicked THEN error message is sent and no callback`() = runTest {
        // Arrange
        coEvery { enrollPromoCampaignUseCase.invoke(any(), any(), any()) } returns
            Either.Left(RuntimeException("network"))
        val model = createModel(CampaignType.WhaleSwapCashback(campaignId = "1"))
        advanceUntilIdle()

        // Act
        onCurrencyChosen.send(chooseTokenResult(currency = token()))
        advanceUntilIdle()
        model.uiState.value.footerUM.onPrimaryButtonClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 0) { modelCallbacks.onActivated(any()) }
        verify(exactly = 0) { modelCallbacks.onAlreadyActivated(any()) }
        model.onDestroy()
    }

    private fun chooseTokenResult(currency: CryptoCurrency): ChooseTokenResult {
        val status = CryptoCurrencyStatus(
            currency = currency,
            // Must carry a networkAddress: the model resolves userAddress from it and otherwise drops the token.
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = null,
                fiatRate = null,
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = userAddress, type = NetworkAddress.Address.Type.Primary),
                ),
            ),
        )
        val wallet: UserWallet = mockk {
            every { walletId } returns userWalletId
        }
        return ChooseTokenResult(currency = status, account = mockk(relaxed = true), wallet = wallet)
    }

    private fun TestScope.createModel(campaignType: CampaignType): ActivateCampaignsModel {
        onCurrencyChosen = Channel(capacity = Channel.UNLIMITED)
        val bridge = mockk<ChooseTokenBridge>(relaxed = true) {
            every { onCurrencyChosen } returns this@ActivateCampaignsModelTest.onCurrencyChosen
            every { onClose } returns Channel()
        }
        every { chooseTokenBridgeFactory.create(any(), any(), any()) } returns bridge
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { multiAccountListSupplier.invoke() } returns flowOf(emptyList<AccountList>())
        coEvery { getPromoCampaignStateUseCase(any(), any(), any()) } returns Either.Left(Throwable())
        every { getWalletsUseCase.invokeSync() } returns allWalletIds.map { walletId ->
            mockk<UserWallet> { every { this@mockk.walletId } returns walletId }
        }
        return ActivateCampaignsModel(
            paramsContainer = MutableParamsContainer(
                ActivateCampaignBottomSheetComponent.Params(
                    campaignType = campaignType,
                    userWalletId = userWalletId,
                    modelCallbacks = modelCallbacks,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            chooseTokenBridgeFactory = chooseTokenBridgeFactory,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            multiAccountListSupplier = multiAccountListSupplier,
            enrollPromoCampaignUseCase = enrollPromoCampaignUseCase,
            urlOpener = urlOpener,
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            getPromoCampaignStateUseCase = getPromoCampaignStateUseCase,
            predefinedTokenResolver = predefinedTokenResolver,
            getWalletsUseCase = getWalletsUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    // Override name/symbol so analytics assertions pin the symbol ("TTK"), not the name ("TEST_TOKEN").
    private fun token(): CryptoCurrency.Token = cryptoCurrencyFactory
        .createToken(blockchain = Blockchain.Ethereum, contractAddress = "0xToken")
        .copy(name = "TEST_TOKEN", symbol = "TTK")

    private fun coin(): CryptoCurrency.Coin = cryptoCurrencyFactory.ethereum

    private fun provideCampaignTypes() = listOf(
        CampaignType.WhaleSwapCashback(campaignId = "1"),
        CampaignType.ReactivationCashback(campaignId = "2"),
    )

    private companion object {
        val userWalletId = UserWalletId("0011223344556677")

        // The user's payout address the model resolves from the chosen token's networkAddress.
        const val userAddress = "0xUserPayoutAddress"

        // Enrollment must target ALL user wallets ([REDACTED_TASK_KEY]), not only the currently selected one.
        val allWalletIds = listOf(
            userWalletId,
            UserWalletId("8899aabbccddeeff"),
            UserWalletId("a1b2c3d4e5f60718"),
        )
    }
}