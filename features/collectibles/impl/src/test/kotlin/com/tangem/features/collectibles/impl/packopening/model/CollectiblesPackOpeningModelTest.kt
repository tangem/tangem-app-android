package com.tangem.features.collectibles.impl.packopening.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import coil.request.Disposable
import coil.request.ImageResult
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.BuybackError
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.usecase.BuybackCollectibleUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleBuybackOfferUseCase
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.collectibles.usecase.OpenCollectiblePackUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.createMachine
import com.tangem.features.collectibles.impl.createOpenedPack
import com.tangem.features.collectibles.impl.packopening.model.converter.CollectiblesRewardCardConverter
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.CollectiblesPackOpeningComponent
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.SellBackButtonUM
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.Stage
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.collectibles.MockCollectibles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class CollectiblesPackOpeningModelTest {

    private val getMachineUseCase: GetCollectibleGachaMachineUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()
    private val openCollectiblePackUseCase: OpenCollectiblePackUseCase = mockk()
    private val getBuybackOfferUseCase: GetCollectibleBuybackOfferUseCase = mockk()
    private val buybackCollectibleUseCase: BuybackCollectibleUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val userWallet = MockUserWalletFactory.create().copy(walletId = USER_WALLET_ID)
    private val statusFlow = MutableStateFlow<CollectiblesAccountStatusValue>(onboarded(usdcAmount = "1000"))
    private val player: PackRevealVideoPlayer = mockk(relaxed = true)
    private val imagePreloader: ImagePreloader = mockk()
    private val playerFactory: PackRevealVideoPlayer.Factory = mockk()
    private val videoLoadState = MutableStateFlow(VideoLoadState.Loading)
    private val playerListener = slot<PackRevealVideoPlayer.Listener>()

    private var model: CollectiblesPackOpeningModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(
            getMachineUseCase,
            router,
            accountStatusSupplier,
            openCollectiblePackUseCase,
            getBuybackOfferUseCase,
            buybackCollectibleUseCase,
            userWalletsListRepository,
            messageSender,
            player,
            playerFactory,
            imagePreloader,
        )
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Opened(PACK))
        statusFlow.value = onboarded(usdcAmount = "1000")
        videoLoadState.value = VideoLoadState.Loading
        every { playerFactory.create(capture(playerListener)) } returns player
        stubCardImages(CompletableDeferred(mockk<ImageResult>()))
        every { player.videoLoadState } returns videoLoadState
        coEvery { getMachineUseCase(any(), any()) } returns createMachine().right()
        every { accountStatusSupplier(USER_WALLET_ID) } returns statusFlow
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN the machine loads WHEN model created THEN the order is rendered`() = runTest {
        // Act
        val model = startModel()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CollectiblesPackOpeningUM.Content::class.java)

        val content = state as CollectiblesPackOpeningUM.Content
        assertThat(content.order.rows).hasSize(3)
        coVerify(exactly = 1) { getMachineUseCase(externalId = EXTERNAL_ID, skipCache = false) }
    }

    @Test
    fun `GIVEN the machine fails WHEN model created THEN the screen reports the error`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns CollectiblesError.Api(IllegalStateException()).left()

        // Act
        val model = startModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesPackOpeningUM.Error::class.java)
    }

    @Test
    fun `GIVEN the machine failed WHEN reload tapped twice THEN one cache-bypassing retry runs with a spinner`() =
        runTest {
            // Arrange
            coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
            coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { CompletableDeferred<MachineResult>().await() }
            val model = startModel()
            val error = model.uiState.value as CollectiblesPackOpeningUM.Error

            // Act
            error.onReloadClick()
            error.onReloadClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value).isEqualTo(error.copy(isReloading = true))
            coVerify(exactly = 1) { getMachineUseCase(EXTERNAL_ID, true) }
        }

    @Test
    fun `GIVEN a retry in flight WHEN it succeeds THEN the order is rendered`() = runTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        (model.uiState.value as CollectiblesPackOpeningUM.Error).onReloadClick()
        runCurrent()

        // Act
        retry.complete(createMachine().right())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesPackOpeningUM.Content::class.java)
    }

    @Test
    fun `GIVEN a retry in flight WHEN it fails again THEN the error is shown without the spinner`() = runTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        val error = model.uiState.value as CollectiblesPackOpeningUM.Error
        error.onReloadClick()
        runCurrent()

        // Act
        retry.complete(CollectiblesError.Api(IllegalStateException()).left())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(error)
    }

    @Test
    fun `GIVEN the pack is open WHEN back tapped THEN the flow is left`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        coVerify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN the reward card WHEN keep card tapped THEN machine details opens`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.reveal().onKeepCardClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN the order WHEN close tapped THEN machine details closes with it`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.popTo(CollectiblesRoute.Main(USER_WALLET_ID), any()) }
    }

    @Test
    fun `GIVEN the order WHEN approve tapped THEN the pack is bought with the route params`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            openCollectiblePackUseCase(userWallet, OpenPackRequest.Buy(EXTERNAL_ID, isTurboEnabled = false))
        }
        assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
    }

    @Test
    fun `GIVEN the pack opens WHEN the reveal launches THEN the card is the won one`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.reveal().card)
            .isEqualTo(CollectiblesRewardCardConverter.convert(pack = PACK))
    }

    @Test
    fun `GIVEN a purchase not finished WHEN state read THEN there is no card yet`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flow {
            emit(OpenPackStep.Opening)
            awaitCancellation()
        }
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.reveal().card).isNull()
    }

    @Test
    fun `GIVEN the vault is short of the total WHEN approve tapped THEN nothing is bought`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        ).right()
        statusFlow.value = onboarded(usdcAmount = "50.99")
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { openCollectiblePackUseCase(any(), any()) }
        verify(exactly = 1) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN a purchase in progress WHEN state read THEN the button is a disabled progress label`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flow {
            emit(OpenPackStep.Confirming)
            awaitCancellation()
        }
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.order().button).isEqualTo(
            CollectiblesPackOpeningUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_confirming_transaction_button),
                isEnabled = false,
            ),
        )
        assertThat(model.reveal().stage).isEqualTo(Stage.Order)
    }

    @Test
    fun `GIVEN a purchase in progress WHEN back tapped THEN the screen stays`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flow {
            emit(OpenPackStep.Confirming)
            awaitCancellation()
        }
        val model = startModel()
        model.content().onApproveClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN a failed purchase whose flow is still open WHEN back tapped THEN the flow is left`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flow {
            emit(OpenPackStep.Failed.PurchaseFailed)
            awaitCancellation()
        }
        val model = startModel()
        model.content().onApproveClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN a purchase in progress WHEN close tapped THEN the screen stays`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flow {
            emit(OpenPackStep.Confirming)
            awaitCancellation()
        }
        val model = startModel()
        model.content().onApproveClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 0) { router.popTo(any<Route>(), any()) }
    }

    @Test
    fun `GIVEN a failure before the payment WHEN approve tapped THEN a snackbar is shown`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Failed.PurchaseFailed)
        val message = slot<EventMessage>()
        every { messageSender.send(capture(message)) } returns Unit
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(message.captured).isInstanceOf(SnackbarMessage::class.java)
        assertThat(model.reveal().stage).isEqualTo(Stage.Order)
    }

    @Test
    fun `GIVEN the pack is left unopened WHEN approve tapped THEN its sheet is shown`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Failed.PackUnopened(TX_ID))
        val model = startModel()
        var shownSheet: CollectiblesConfirmOrderBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(shownSheet).isEqualTo(CollectiblesConfirmOrderBottomSheetConfig.PackUnopened(TX_ID))
    }

    @Test
    fun `GIVEN a sheet WHEN it leaves to main THEN the flow returns to main`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.onExplorePacks()

        // Assert
        verify(exactly = 1) { router.popTo(CollectiblesRoute.Main(USER_WALLET_ID), any()) }
    }

    @Test
    fun `GIVEN the pack is left unopened WHEN open pack tapped THEN it is opened again in place`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(userWallet, OpenPackRequest.Reopen(TX_ID)) } returns
            flowOf(OpenPackStep.Opened(PACK))
        val model = startModel()

        // Act
        model.onReopenPack(TX_ID)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { openCollectiblePackUseCase(userWallet, OpenPackRequest.Reopen(TX_ID)) }
        verify(exactly = 0) { router.popTo(any<Route>(), any()) }
        assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
    }

    @Test
    fun `GIVEN a purchase that has not reported a step WHEN approve tapped twice THEN one purchase runs`() =
        runTest {
            // Arrange
            every { openCollectiblePackUseCase(any(), any()) } returns flow { awaitCancellation() }
            val model = startModel()

            // Act
            model.content().onApproveClick()
            model.content().onApproveClick()
            runCurrent()

            // Assert
            verify(exactly = 1) { openCollectiblePackUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN a failed purchase WHEN approve tapped again THEN it is retried`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Failed.PurchaseFailed)
        val model = startModel()
        model.content().onApproveClick()
        advanceUntilIdle()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) { openCollectiblePackUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN the wallet is gone WHEN approve tapped THEN the purchase fails and can be tapped again`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(emptyList())
        val model = startModel()
        model.content().onApproveClick()
        advanceUntilIdle()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) {
            messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_something_went_wrong)))
        }
        verify(exactly = 0) { openCollectiblePackUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN low inventory WHEN approve tapped THEN its sheet is shown`() = runTest {
        // Arrange
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Failed.LowInventory)
        val model = startModel()
        var shownSheet: CollectiblesConfirmOrderBottomSheetConfig? = null
        model.bottomSheetNavigation.subscribe { event -> shownSheet = event.transformer(shownSheet) }

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(shownSheet).isEqualTo(CollectiblesConfirmOrderBottomSheetConfig.LowInventory)
    }

    @Test
    fun `GIVEN the low inventory sheet WHEN enable turbo tapped THEN the flow goes one screen back`() = runTest {
        // Arrange
        val model = startModel()

        // Act
        model.onEnableTurboMode()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN the vault covers the total WHEN model created THEN approve is available`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        ).right()
        statusFlow.value = onboarded(usdcAmount = "51")

        // Act
        val model = startModel()

        // Assert
        assertThat(model.order().button.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN the vault is short of the total WHEN model created THEN approve is blocked`() = runTest {
        // Arrange
        coEvery { getMachineUseCase(any(), any()) } returns createMachine(
            price = BigDecimal("50"),
            feeAmount = BigDecimal("1"),
        ).right()
        statusFlow.value = onboarded(usdcAmount = "50.99")

        // Act
        val model = startModel()

        // Assert
        assertThat(model.order().button.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN approve is blocked WHEN the vault is topped up THEN approve becomes available`() = runTest {
        // Arrange
        statusFlow.value = onboarded(usdcAmount = "0")
        val model = startModel()

        // Act
        statusFlow.value = onboarded(usdcAmount = "1000")
        advanceUntilIdle()

        // Assert
        assertThat(model.order().button.isEnabled).isTrue()
    }

    @Nested
    inner class Reveal {

        @Test
        fun `GIVEN the pack opens WHEN the reveal launches THEN both card faces are preloaded`() = runTest {
            // Arrange
            val model = startModel()

            // Act
            model.content().onApproveClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { imagePreloader.preload(PACK.imageFrontUrl!!) }
            verify(exactly = 1) { imagePreloader.preload(PACK.imageBackUrl!!) }
        }

        @Test
        fun `GIVEN the short reveal WHEN the card images are still loading THEN the pack waits for them`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Failed
            val images = CompletableDeferred<ImageResult>()
            stubCardImages(images)
            val model = startModel()
            model.content().onApproveClick()
            runCurrent()
            val stageWhileLoading = model.reveal().stage

            // Act
            images.complete(mockk())
            runCurrent()

            // Assert
            assertThat(stageWhileLoading).isEqualTo(Stage.Order)
            assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
        }

        @Test
        fun `GIVEN the short reveal WHEN the card images never load THEN the pack is thrown after the timeout`() =
            runTest {
                // Arrange
                videoLoadState.value = VideoLoadState.Failed
                stubCardImages(CompletableDeferred())
                val model = startModel()
                model.content().onApproveClick()

                // Act
                advanceTimeBy(PackOpeningConst.CARD_IMAGES_TIMEOUT_MS + 1)

                // Assert
                assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
                assertThat(model.reveal().isFullReveal).isFalse()
            }

        @Test
        fun `GIVEN a ready video WHEN the card images never load THEN the full reveal launches after the timeout`() =
            runTest {
                // Arrange
                videoLoadState.value = VideoLoadState.Ready
                stubCardImages(CompletableDeferred())
                val model = startModel()
                model.content().onApproveClick()
                runCurrent()
                val stageWhileLoading = model.reveal().stage

                // Act
                advanceTimeBy(PackOpeningConst.CARD_IMAGES_TIMEOUT_MS + 1)

                // Assert
                assertThat(stageWhileLoading).isEqualTo(Stage.Order)
                assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
                assertThat(model.reveal().isFullReveal).isTrue()
            }

        @Test
        fun `GIVEN the machine loads WHEN model created THEN its skip sound is preloaded`() = runTest {
            // Arrange
            coEvery { getMachineUseCase(any(), any()) } returns createMachine(revealAssets = REVEAL_ASSETS).right()

            // Act
            startModel()

            // Assert
            coVerify(exactly = 1) { player.preloadSkipSound(SKIP_AUDIO_URL) }
        }

        @Test
        fun `GIVEN a machine without reveal media WHEN the theme is reported THEN the player gets no media`() = runTest {
            // Act
            startModel(isDarkTheme = false)

            // Assert
            coVerify(exactly = 1) { player.preloadSkipSound(null) }
            coVerify(exactly = 1) { player.preloadVideo(null) }
        }

        @Test
        fun `GIVEN a ready video WHEN approve tapped THEN the full reveal launches with the video`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()

            // Act
            model.content().onApproveClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
            assertThat(model.reveal().isFullReveal).isTrue()
            verify(exactly = 1) { player.start() }
        }

        @Test
        fun `GIVEN the video never gets ready WHEN approve tapped THEN the short reveal launches after the timeout`() =
            runTest {
                // Arrange
                val model = startModel()

                // Act
                model.content().onApproveClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
                assertThat(model.reveal().isFullReveal).isFalse()
                verify(exactly = 0) { player.start() }
            }

        @Test
        fun `GIVEN the video failed WHEN approve tapped THEN the short reveal launches`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Failed
            val model = startModel()

            // Act
            model.content().onApproveClick()
            runCurrent()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
            assertThat(model.reveal().isFullReveal).isFalse()
        }

        @Test
        fun `GIVEN the full reveal WHEN the pack animation finishes THEN the video plays until the card reveal`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()

            // Act
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Reveal)
            verify(exactly = 0) { player.playSkipSound() }
        }

        @Test
        fun `GIVEN the video is playing WHEN the card reveal is reached THEN the card is shown`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Act
            playerListener.captured.onCardRevealReached()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Reward)
        }

        @Test
        fun `GIVEN the card reveal came during the throw WHEN the pack animation finishes THEN the card is shown`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()
            playerListener.captured.onCardRevealReached()

            // Act
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Reward)
        }

        @Test
        fun `GIVEN the short reveal WHEN the pack animation finishes THEN the card is shown with the skip sound`() =
            runTest {
                // Arrange
                videoLoadState.value = VideoLoadState.Failed
                val model = startModel()
                model.content().onApproveClick()
                advanceUntilIdle()

                // Act
                model.reveal().onPackAnimationFinished()
                advanceUntilIdle()

                // Assert
                assertThat(model.reveal().stage).isEqualTo(Stage.Reward)
                verify(exactly = 1) { player.playSkipSound() }
            }

        @Test
        fun `GIVEN the video is playing WHEN the screen is tapped THEN the reveal skips to the burst`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            every { player.skipToReveal() } returns true
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Act
            model.reveal().onRevealTap()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { player.skipToReveal() }
            assertThat(model.reveal().isSkipped).isTrue()
        }

        @Test
        fun `GIVEN the burst has begun WHEN the screen is tapped THEN the reveal is not marked skipped`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            every { player.skipToReveal() } returns false
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Act
            model.reveal().onRevealTap()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().isSkipped).isFalse()
        }

        @Test
        fun `GIVEN the pack is still being thrown WHEN the screen is tapped THEN nothing is skipped`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()

            // Act
            model.reveal().onRevealTap()

            // Assert
            verify(exactly = 0) { player.skipToReveal() }
        }

        @Test
        fun `GIVEN the model WHEN destroyed THEN the player is released`() = runTest {
            // Arrange
            val model = startModel()

            // Act
            model.onDestroy()
            this@CollectiblesPackOpeningModelTest.model = null

            // Assert
            verify(exactly = 1) { player.release() }
        }

        @Test
        fun `GIVEN the opened pack waits for the video WHEN back tapped THEN the screen stays`() = runTest {
            // Arrange
            val model = startModel()
            model.content().onApproveClick()
            runCurrent()

            // Act
            model.uiState.value.onBackClick()
            model.uiState.value.onCloseClick()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Order)
            verify(exactly = 0) { router.pop() }
            verify(exactly = 0) { router.popTo(any<Route>(), any()) }
        }

        @Test
        fun `GIVEN the pack is being thrown WHEN back tapped THEN the screen stays`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()

            // Act
            model.uiState.value.onBackClick()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Launching)
            verify(exactly = 0) { router.pop() }
        }

        @Test
        fun `GIVEN the card is shown WHEN back tapped THEN the screen is left`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Failed
            val model = startModel()
            model.content().onApproveClick()
            advanceUntilIdle()
            model.reveal().onPackAnimationFinished()
            advanceUntilIdle()

            // Act
            model.uiState.value.onBackClick()

            // Assert
            assertThat(model.reveal().stage).isEqualTo(Stage.Reward)
            verify(exactly = 1) { router.pop() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Theme {

        @BeforeEach
        fun stubRevealAssets() {
            coEvery { getMachineUseCase(any(), any()) } returns createMachine(revealAssets = REVEAL_ASSETS).right()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the app theme WHEN the machine loads THEN the video of that theme is preloaded`(
            testModel: ThemeVideoModel,
        ) = runTest {
            // Act
            startModel(isDarkTheme = testModel.isDarkTheme)

            // Assert
            coVerify(exactly = 1) { player.preloadVideo(PackRevealAsset(videoUrl = testModel.videoUrl)) }
        }

        @Test
        fun `GIVEN no theme reported yet WHEN the machine loads THEN no video is preloaded`() = runTest {
            // Act
            startModel(isDarkTheme = null)

            // Assert
            coVerify(exactly = 0) { player.preloadVideo(any()) }
        }

        @Test
        fun `GIVEN the order is shown WHEN the theme changes THEN the other video is preloaded`() = runTest {
            // Arrange
            val model = startModel(isDarkTheme = false)

            // Act
            model.onThemeChanged(isDarkTheme = true)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { player.preloadVideo(PackRevealAsset(videoUrl = DARK_VIDEO_URL)) }
        }

        @Test
        fun `GIVEN a video still loading WHEN the theme changes THEN its load is cancelled`() = runTest {
            // Arrange
            var isLightLoadCancelled = false
            coEvery { player.preloadVideo(PackRevealAsset(videoUrl = LIGHT_VIDEO_URL)) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    isLightLoadCancelled = true
                }
            }
            val model = startModel(isDarkTheme = false)

            // Act
            model.onThemeChanged(isDarkTheme = true)
            advanceUntilIdle()

            // Assert
            assertThat(isLightLoadCancelled).isTrue()
        }

        @Test
        fun `GIVEN the video awaited after approve WHEN the theme changes THEN the other video is preloaded`() =
            runTest {
                // Arrange
                val model = startModel(isDarkTheme = false)
                model.content().onApproveClick()
                runCurrent()

                // Act
                model.onThemeChanged(isDarkTheme = true)
                runCurrent()

                // Assert
                coVerify(exactly = 1) { player.preloadVideo(PackRevealAsset(videoUrl = DARK_VIDEO_URL)) }
            }

        @Test
        fun `GIVEN the reveal launched WHEN the theme changes THEN the video is kept`() = runTest {
            // Arrange
            videoLoadState.value = VideoLoadState.Ready
            val model = startModel(isDarkTheme = false)
            model.content().onApproveClick()
            advanceUntilIdle()

            // Act
            model.onThemeChanged(isDarkTheme = true)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { player.preloadVideo(PackRevealAsset(videoUrl = DARK_VIDEO_URL)) }
        }

        @Test
        fun `GIVEN a video still loading WHEN the short reveal launches THEN its load is cancelled`() = runTest {
            // Arrange
            var isLoadCancelled = false
            coEvery { player.preloadVideo(any()) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    isLoadCancelled = true
                }
            }
            val model = startModel(isDarkTheme = false)

            // Act
            model.content().onApproveClick()
            advanceUntilIdle()

            // Assert
            assertThat(isLoadCancelled).isTrue()
        }

        private fun provideTestModels() = listOf(
            ThemeVideoModel(isDarkTheme = false, videoUrl = LIGHT_VIDEO_URL),
            ThemeVideoModel(isDarkTheme = true, videoUrl = DARK_VIDEO_URL),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SellBack {

        @BeforeEach
        fun setUpBuyback() {
            coEvery { getBuybackOfferUseCase(USER_WALLET_ID, PACK.nftAddress) } returns OFFER.right()
            videoLoadState.value = VideoLoadState.Failed
        }

        @Test
        fun `GIVEN an offer WHEN the pack opens THEN sell back shows its amount`() = runTest {
            // Act
            val model = openPack()

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(AVAILABLE_BUTTON)
            coVerify(exactly = 1) { getBuybackOfferUseCase(USER_WALLET_ID, PACK.nftAddress) }
        }

        @Test
        fun `GIVEN buyback not available WHEN five seconds pass THEN it is asked again and shown`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returnsMany listOf(null.right(), OFFER.right())
            val model = openPack(isOfferSettled = false)
            assertThat(model.reveal().sellBack).isEqualTo(UNAVAILABLE_BUTTON)

            // Act
            advanceTimeBy(BUYBACK_POLLING_DELAY_MS + 1)

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(AVAILABLE_BUTTON)
            coVerify(exactly = 2) { getBuybackOfferUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN a failed check WHEN five seconds pass THEN it is asked again`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returnsMany listOf(
                CollectiblesError.Api(IllegalStateException()).left(),
                OFFER.right(),
            )
            val model = openPack(isOfferSettled = false)
            assertThat(model.reveal().sellBack).isEqualTo(UNAVAILABLE_BUTTON)

            // Act
            advanceTimeBy(BUYBACK_POLLING_DELAY_MS + 1)

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(AVAILABLE_BUTTON)
        }

        @Test
        fun `GIVEN the screen is paused WHEN the pack opens THEN nothing is asked until it resumes`() = runTest {
            // Arrange
            val model = openPack(isResumed = false)
            coVerify(exactly = 0) { getBuybackOfferUseCase(any(), any()) }

            // Act
            model.onResume()
            advanceUntilIdle()

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(AVAILABLE_BUTTON)
        }

        @Test
        fun `GIVEN polling WHEN the screen pauses THEN no more requests are sent`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returns null.right()
            val model = openPack(isOfferSettled = false)

            // Act
            model.onPause()
            advanceTimeBy(BUYBACK_POLLING_DELAY_MS * 3)

            // Assert
            coVerify(exactly = 1) { getBuybackOfferUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN the sale completes WHEN sell back tapped THEN machine details opens with a snackbar`() = runTest {
            // Arrange
            coEvery { buybackCollectibleUseCase(userWallet, PACK.nftAddress) } returns Unit.right()
            val model = openPack()

            // Act
            model.reveal().onSellBackClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                messageSender.send(
                    SnackbarMessage(
                        message = resourceReference(
                            id = R.string.collectibles_open_pack_sell_back_success,
                            formatArgs = wrappedList("$42.50"),
                        ),
                    ),
                )
                router.pop()
            }
        }

        @Test
        fun `GIVEN a sale in progress WHEN back, close or keep card tapped THEN the screen stays`() = runTest {
            // Arrange
            coEvery { buybackCollectibleUseCase(any(), any()) } coAnswers { awaitCancellation() }
            val model = openPack()

            // Act
            model.reveal().onSellBackClick()
            runCurrent()
            model.uiState.value.onBackClick()
            model.uiState.value.onCloseClick()
            model.reveal().onKeepCardClick()

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(IN_PROGRESS_BUTTON)
            assertThat(model.reveal().isKeepCardEnabled).isFalse()
            verify(exactly = 0) {
                router.pop()
                router.popTo(any<Route>(), any())
            }
        }

        @Test
        fun `GIVEN a sale in progress WHEN sell back tapped again THEN no second sale starts`() = runTest {
            // Arrange
            coEvery { buybackCollectibleUseCase(any(), any()) } coAnswers { awaitCancellation() }
            val model = openPack()

            // Act
            model.reveal().onSellBackClick()
            runCurrent()
            model.reveal().onSellBackClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { buybackCollectibleUseCase(any(), any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed sale WHEN sell back tapped THEN the user stays on the card`(model: FailedSaleModel) =
            runTest {
                // Arrange
                coEvery { buybackCollectibleUseCase(any(), any()) } returns model.failure.left()
                val packOpening = openPack()

                // Act
                packOpening.reveal().onSellBackClick()
                runCurrent()

                // Assert
                assertThat(packOpening.reveal().sellBack).isEqualTo(model.button)
                if (model.snackbar == null) {
                    verify(exactly = 0) { messageSender.send(any()) }
                } else {
                    verify(exactly = 1) { messageSender.send(SnackbarMessage(message = model.snackbar)) }
                }
                verify(exactly = 0) { router.pop() }
                packOpening.onDestroy()
            }

        private fun provideTestModels() = listOf(
            FailedSaleModel(
                failure = BuybackError.Cancelled,
                button = AVAILABLE_BUTTON,
                snackbar = null,
            ),
            FailedSaleModel(
                failure = BuybackError.CardRefused,
                button = AVAILABLE_BUTTON,
                snackbar = null,
            ),
            FailedSaleModel(
                failure = BuybackError.Unexpected,
                button = AVAILABLE_BUTTON,
                snackbar = resourceReference(R.string.unexpected_error_description),
            ),
        )

        @Test
        fun `GIVEN the sale is not confirmed WHEN sell back tapped THEN the offer is checked again`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returnsMany listOf(OFFER.right(), null.right())
            coEvery { buybackCollectibleUseCase(any(), any()) } returns BuybackError.NotConfirmed.left()
            val model = openPack()

            // Act
            model.reveal().onSellBackClick()
            runCurrent()

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(UNAVAILABLE_BUTTON)
            assertThat(model.reveal().isKeepCardEnabled).isTrue()
            verify(exactly = 1) {
                messageSender.send(SnackbarMessage(message = resourceReference(R.string.unexpected_error_description)))
            }
            coVerify(exactly = 2) { getBuybackOfferUseCase(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the sale is not confirmed WHEN the offer comes back THEN sell back is available again`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returnsMany
                listOf(OFFER.right(), null.right(), OFFER.right())
            coEvery { buybackCollectibleUseCase(any(), any()) } returns BuybackError.NotConfirmed.left()
            val model = openPack()
            model.reveal().onSellBackClick()
            runCurrent()

            // Act
            advanceTimeBy(BUYBACK_POLLING_DELAY_MS + 1)

            // Assert
            assertThat(model.reveal().sellBack).isEqualTo(AVAILABLE_BUTTON)
        }

        @Test
        fun `GIVEN no offer yet WHEN keep card tapped THEN machine details opens`() = runTest {
            // Arrange
            coEvery { getBuybackOfferUseCase(any(), any()) } returns null.right()
            val model = openPack(isOfferSettled = false)

            // Act
            model.reveal().onKeepCardClick()

            // Assert
            assertThat(model.reveal().isKeepCardEnabled).isTrue()
            verify(exactly = 1) { router.pop() }
            model.onDestroy()
        }

        /**
         * Opens the pack and lands on the card. With the offer still unknown polling never goes idle, so the
         * clock is moved only as far as the short reveal needs.
         */
        private fun TestScope.openPack(
            isResumed: Boolean = true,
            isOfferSettled: Boolean = true,
        ): CollectiblesPackOpeningModel {
            val model = startModel()
            if (isResumed) model.onResume()

            model.content().onApproveClick()
            if (isOfferSettled) advanceUntilIdle() else runCurrent()
            model.reveal().onPackAnimationFinished()
            runCurrent()

            return model
        }
    }

    internal data class FailedSaleModel(
        val failure: BuybackError,
        val button: SellBackButtonUM,
        val snackbar: TextReference?,
    )

    internal data class ThemeVideoModel(val isDarkTheme: Boolean, val videoUrl: String)

    private fun stubCardImages(job: CompletableDeferred<ImageResult>) {
        val load = mockk<Disposable>()
        every { load.job } returns job
        every { imagePreloader.preload(any()) } returns load
    }

    private fun CollectiblesPackOpeningModel.content() = uiState.value as CollectiblesPackOpeningUM.Content

    private fun CollectiblesPackOpeningModel.reveal() = content().reveal

    private fun CollectiblesPackOpeningModel.order() = (uiState.value as CollectiblesPackOpeningUM.Content).order

    private fun onboarded(usdcAmount: String) = CollectiblesAccountStatusValue.Onboarded(
        source = StatusSource.ACTUAL,
        vaultBalance = MockCollectibles.vaultBalance(amount = BigDecimal(usdcAmount), fiatRate = BigDecimal.ONE),
    )

    private fun TestScope.startModel(isDarkTheme: Boolean? = null): CollectiblesPackOpeningModel {
        val model = CollectiblesPackOpeningModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            accountStatusSupplier = accountStatusSupplier,
            openCollectiblePackUseCase = openCollectiblePackUseCase,
            getBuybackOfferUseCase = getBuybackOfferUseCase,
            buybackCollectibleUseCase = buybackCollectibleUseCase,
            userWalletsListRepository = userWalletsListRepository,
            messageSender = messageSender,
            router = router,
            imagePreloader = imagePreloader,
            playerFactory = playerFactory,
            paramsContainer = MutableParamsContainer(
                CollectiblesPackOpeningComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    externalId = EXTERNAL_ID,
                    isTurboEnabled = false,
                ),
            ),
        ).also { this@CollectiblesPackOpeningModelTest.model = it }

        if (isDarkTheme != null) model.onThemeChanged(isDarkTheme)
        advanceUntilIdle()

        return model
    }

    private companion object {

        const val TX_ID = "tx-id"

        const val EXTERNAL_ID = "pokemon_1"
        val USER_WALLET_ID = UserWalletId("011")

        const val LIGHT_VIDEO_URL = "https://cdn/light.mp4"
        const val DARK_VIDEO_URL = "https://cdn/dark.mp4"
        const val SKIP_AUDIO_URL = "https://cdn/skip.mp3"
        val REVEAL_ASSETS = CollectibleGachaMachine.RevealAssets(
            lightVideoUrl = LIGHT_VIDEO_URL,
            darkVideoUrl = DARK_VIDEO_URL,
            skipAudioUrl = SKIP_AUDIO_URL,
        )

        val PACK = createOpenedPack(txId = TX_ID)

        const val BUYBACK_POLLING_DELAY_MS = 5_000L
        val AMOUNT = BigDecimal("42.50")
        val OFFER = CollectibleBuybackOffer(amount = AMOUNT, expiresAt = null)

        val AVAILABLE_BUTTON = SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_sell_back_button, wrappedList("$42.50")),
            isEnabled = true,
            isLoading = false,
        )
        val IN_PROGRESS_BUTTON = AVAILABLE_BUTTON.copy(isLoading = true)
        val UNAVAILABLE_BUTTON = SellBackButtonUM(
            textRef = resourceReference(R.string.collectibles_open_pack_buyback_unavailable),
            isEnabled = false,
            isLoading = false,
        )
    }
}

private typealias MachineResult = Either<CollectiblesError, CollectibleGachaMachine>