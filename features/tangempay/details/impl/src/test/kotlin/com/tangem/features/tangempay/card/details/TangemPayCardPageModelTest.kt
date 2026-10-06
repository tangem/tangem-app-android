package com.tangem.features.tangempay.card.details

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletController
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletController.Entry as GoogleWalletEntry
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayCardPageModelTest {

    private val userWalletId = UserWalletId("123")
    private val firstCard = tangemPayCard(id = "card_a")
    private val secondCard = tangemPayCard(id = "card_b")

    private val eventListener = DefaultCardDetailsEventListener()
    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val controllerFactory: TangemPayCardDetailsController.Factory = mockk()
    private val googleWalletController: TangemPayAddToGoogleWalletController = mockk(relaxed = true) {
        every { entry } returns MutableStateFlow(TangemPayAddToGoogleWalletController.Entry.ManualGuide)
    }
    private val googleWalletControllerFactory: TangemPayAddToGoogleWalletController.Factory = mockk {
        every { create(any(), any()) } returns googleWalletController
    }

    private val loadedStatus: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns StatusSource.ACTUAL
        every { cards } returns listOf(firstCard, secondCard)
    }

    private val paymentStatus: AccountStatus.Payment = mockk(relaxed = true) {
        every { account } returns Account.Payment(userWalletId)
        every { value } returns loadedStatus
    }

    init {
        every { supplier.invoke(userWalletId) } returns statusFlow
        every { cardDetailsRepository.cardFrozenState(any()) } returns emptyFlow()
        coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns true.right()
        every {
            controllerFactory.create(any(), any(), any(), any(), any(), any())
        } answers {
            val card = secondArg<TangemPayCard>()
            mockk(relaxed = true) { every { cardId } returns card.id }
        }
    }

    @Test
    fun `GIVEN details shown WHEN pager moves to another card THEN details row is enabled`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        model.onCardPageSelected(index = 1)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isTrue()
    }

    @Test
    fun `GIVEN details shown WHEN pager returns to that card THEN details row stays disabled`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        model.onCardPageSelected(index = 1)
        runCurrent()
        model.onCardPageSelected(index = 0)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isFalse()
    }

    @Test
    fun `GIVEN details shown WHEN frozen state re-emitted THEN details row stays disabled`() = runTest {
        // Arrange
        val frozenStateFlow = MutableSharedFlow<TangemPayCardFrozenState>(replay = 1)
        every { cardDetailsRepository.cardFrozenState(firstCard.id) } returns frozenStateFlow
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        frozenStateFlow.emit(TangemPayCardFrozenState.Unfrozen)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isFalse()
    }

    @Test
    fun `GIVEN details hidden WHEN status emitted THEN details row is enabled`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        statusFlow.emit(paymentStatus)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isTrue()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddToWallet {

        @BeforeEach
        fun setUp() {
            clearMocks(googleWalletController, answers = false)
            every { cardDetailsRepository.cardFrozenState(any()) } returns emptyFlow()
            coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns false.right()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Google Wallet entry WHEN model created THEN add-to-wallet blocks follow it`(
            model: AddToWalletBlocksModel,
        ) = runTest {
            // Arrange
            every { googleWalletController.entry } returns MutableStateFlow(model.entry)

            // Act
            val pageModel = createModel()

            // Assert
            assertThat(pageModel.addToWalletBlocks()).isEqualTo(model.expected)
            pageModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            AddToWalletBlocksModel(
                entry = GoogleWalletEntry.Hidden,
                expected = AddToWalletBlocks(isBannerShown = false, button = null),
            ),
            AddToWalletBlocksModel(
                entry = GoogleWalletEntry.ManualGuide,
                expected = AddToWalletBlocks(isBannerShown = true, button = null),
            ),
            AddToWalletBlocksModel(
                entry = GoogleWalletEntry.Button(isLoading = false),
                expected = AddToWalletBlocks(isBannerShown = false, button = ButtonState(isLoading = false)),
            ),
            AddToWalletBlocksModel(
                entry = GoogleWalletEntry.Button(isLoading = true),
                expected = AddToWalletBlocks(isBannerShown = false, button = ButtonState(isLoading = true)),
            ),
        )

        @Test
        fun `GIVEN banner closed WHEN manual guide shown THEN banner hidden`() = runTest {
            // Arrange
            coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns true.right()
            every { googleWalletController.entry } returns MutableStateFlow(GoogleWalletEntry.ManualGuide)

            // Act
            val pageModel = createModel()

            // Assert
            assertThat(pageModel.addToWalletBlocks()).isEqualTo(AddToWalletBlocks(isBannerShown = false, button = null))
            pageModel.onDestroy()
        }

        @Test
        fun `GIVEN actual card status WHEN model created THEN controller gets the card and its frozen state`() =
            runTest {
                // Arrange
                every {
                    cardDetailsRepository.cardFrozenState(firstCard.id)
                } returns flowOf(TangemPayCardFrozenState.Frozen)

                // Act
                val pageModel = createModel()

                // Assert
                verifyOrder {
                    googleWalletController.selectCard(firstCard)
                    googleWalletController.updateFrozenState(firstCard.frozenState)
                    googleWalletController.updateFrozenState(TangemPayCardFrozenState.Frozen)
                }
                pageModel.onDestroy()
            }

        @Test
        fun `GIVEN cached card status WHEN model created THEN controller gets no card`() = runTest {
            // Arrange
            val cachedStatus: AccountStatus.Payment = mockk(relaxed = true) {
                every { account } returns Account.Payment(userWalletId)
                every { value } returns mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
                    every { source } returns StatusSource.ONLY_CACHE
                    every { cards } returns listOf(firstCard, secondCard)
                }
            }

            // Act
            val pageModel = createModel(initialStatus = cachedStatus)

            // Assert
            verify(exactly = 1) { googleWalletController.selectCard(null) }
            verify(exactly = 0) { googleWalletController.selectCard(firstCard) }
            pageModel.onDestroy()
        }

        @Test
        fun `GIVEN Google Wallet button WHEN clicked THEN controller adds the card`() = runTest {
            // Arrange
            every { googleWalletController.entry } returns MutableStateFlow(GoogleWalletEntry.Button(isLoading = false))
            val pageModel = createModel()

            // Act
            checkNotNull(pageModel.uiState.value.addToGoogleWalletButtonState).onClick()

            // Assert
            verify(exactly = 1) { googleWalletController.onAddClick() }
            pageModel.onDestroy()
        }

        @Test
        fun `GIVEN card page WHEN screen started and stopped THEN controller follows the screen`() = runTest {
            // Arrange
            val pageModel = createModel()

            // Act
            pageModel.onStart()
            pageModel.onStop()

            // Assert
            verifyOrder {
                googleWalletController.onStart()
                googleWalletController.onStop()
            }
            pageModel.onDestroy()
        }

        private fun TangemPayCardPageModel.addToWalletBlocks(): AddToWalletBlocks = uiState.value.let { state ->
            AddToWalletBlocks(
                isBannerShown = state.addToWalletBlockState != null,
                button = state.addToGoogleWalletButtonState?.let { ButtonState(isLoading = it.isLoading) },
            )
        }
    }

    internal data class AddToWalletBlocksModel(val entry: GoogleWalletEntry, val expected: AddToWalletBlocks)

    internal data class AddToWalletBlocks(val isBannerShown: Boolean, val button: ButtonState?)

    internal data class ButtonState(val isLoading: Boolean)

    private fun TangemPayCardPageModel.isDetailsSettingEnabled(): Boolean =
        uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Details }.isEnabled

    private fun createModel(initialStatus: AccountStatus.Payment = paymentStatus) = TangemPayCardPageModel(
        paramsContainer = MutableParamsContainer(
            TangemPayCardPageComponent.Params(initialStatus = initialStatus, cardId = firstCard.id),
        ),
        tangemPayCurrencyFactory = mockk(relaxed = true),
        paymentAccountStatusSupplier = supplier,
        paymentAccountStatusFetcher = mockk(relaxed = true),
        dispatchers = TestingCoroutineDispatcherProvider(),
        router = mockk(relaxed = true),
        analytics = mockk(relaxed = true),
        sendFeedbackEmailUseCase = mockk(relaxed = true),
        cardDetailsRepository = cardDetailsRepository,
        uiMessageSender = mockk(relaxed = true),
        changeCardFrozenStateUseCase = mockk(relaxed = true),
        cardDetailsEventListener = eventListener,
        cardDetailsControllerFactory = controllerFactory,
        biometricAuthManager = mockk(relaxed = true),
        settingsManager = mockk(relaxed = true),
        onboardingRepository = mockk(relaxed = true),
        tangemPayFeatureToggles = mockk(relaxed = true),
        googleWalletControllerFactory = googleWalletControllerFactory,
    )
}