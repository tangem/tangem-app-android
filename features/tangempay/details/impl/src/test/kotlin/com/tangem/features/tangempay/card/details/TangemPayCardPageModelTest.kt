package com.tangem.features.tangempay.card.details

import arrow.core.left
import kotlinx.coroutines.CompletableDeferred
import com.tangem.domain.tangempay.usecase.ChangeCardFrozenStateUseCase
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.error.UniversalError
import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayCard
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.usecase.GetBankCredentialsUseCase
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.core.ui.res.generated.icons.ic_snowflake_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_20
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.tangempay.model.CashbackDisplayMode
import com.tangem.domain.tangempay.model.CashbackSummary
import com.tangem.domain.tangempay.model.TangemPayCashback
import com.tangem.domain.tangempay.usecase.ObserveCashbackSummaryUseCase
import com.tangem.features.tangempay.awaitingDepositOrder
import com.tangem.features.tangempay.tariffPlanState
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.card.closure.TangemPayCardCloseCoordinator
import com.tangem.features.tangempay.card.closure.TangemPayClosedCard
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletController
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletController.Entry as GoogleWalletEntry
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.bankCredentials
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.trackSlot
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayCardPageModelTest : TangemPayModelTestBase() {

    private val userWalletId = UserWalletId("123")
    private val firstCard = tangemPayCard(id = "card_a")
    private val secondCard = tangemPayCard(id = "card_b")

    private val eventListener = DefaultCardDetailsEventListener()
    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val getBankCredentialsUseCase: GetBankCredentialsUseCase = mockk()
    private val controllerFactory: TangemPayCardDetailsController.Factory = mockk()
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()
    private val sentMessages = mutableListOf<UiMessage>()
    private val changeCardFrozenStateUseCase: ChangeCardFrozenStateUseCase = mockk()
    private val cardControllers = mutableMapOf<String, TangemPayCardDetailsController>()
    private val googleWalletController: TangemPayAddToGoogleWalletController = mockk(relaxed = true) {
        every { entry } returns MutableStateFlow(TangemPayAddToGoogleWalletController.Entry.ManualGuide)
    }
    private val googleWalletControllerFactory: TangemPayAddToGoogleWalletController.Factory = mockk {
        every { create(any(), any()) } returns googleWalletController
    }
    private val cashbackSummaryFlow = MutableSharedFlow<CashbackSummary?>(replay = 1)
    private val observeCashbackSummaryUseCase: ObserveCashbackSummaryUseCase = mockk()
    private val closedCardsFlow = MutableSharedFlow<TangemPayClosedCard>()
    private val closeCoordinator: TangemPayCardCloseCoordinator = mockk {
        every { closedCards } returns closedCardsFlow
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
        every { observeCashbackSummaryUseCase(userWalletId) } returns cashbackSummaryFlow
        every { cardDetailsRepository.cardFrozenState(any()) } returns emptyFlow()
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false
        every { tangemPayFeatureToggles.isCardActionsEnabled } returns false
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
        coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns true.right()
        every {
            controllerFactory.create(any(), any(), any(), any(), any(), any())
        } answers {
            val card = secondArg<TangemPayCard>()
            mockk<TangemPayCardDetailsController>(relaxed = true) { every { cardId } returns card.id }
                .also { cardControllers[card.id] = it }
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
    fun `GIVEN details shown WHEN screen stopped THEN details row is enabled`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        model.onStop()
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isTrue()
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
    fun `GIVEN order framework enabled and pending card WHEN status emitted THEN freeze row loads from the status`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true
            every { cardDetailsRepository.cardFrozenState(any()) } returns flowOf(TangemPayCardFrozenState.Unfrozen)
            val pendingStatus: AccountStatus.Payment = mockk(relaxed = true) {
                every { account } returns Account.Payment(userWalletId)
                every { value } returns mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
                    every { source } returns StatusSource.ACTUAL
                    every { cards } returns listOf(
                        firstCard.copy(frozenState = TangemPayCardFrozenState.Pending),
                        secondCard,
                    )
                }
            }
            val model = createModel()

            // Act
            statusFlow.emit(pendingStatus)
            runCurrent()

            // Assert
            assertThat(model.uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Freeze }.isLoading)
                .isTrue()
            verify(exactly = 0) { cardDetailsRepository.cardFrozenState(any()) }
        }

    @Test
    fun `GIVEN card pending from restored order WHEN freeze row clicked THEN row disabled and no order started`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true
            val pendingStatus: AccountStatus.Payment = mockk(relaxed = true) {
                every { account } returns Account.Payment(userWalletId)
                every { value } returns mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
                    every { source } returns StatusSource.ACTUAL
                    every { cards } returns listOf(
                        firstCard.copy(frozenState = TangemPayCardFrozenState.Pending),
                        secondCard,
                    )
                }
            }
            val model = createModel()
            statusFlow.emit(pendingStatus)
            runCurrent()

            // Act
            model.freezeRow().onClick()
            runCurrent()

            // Assert
            assertThat(model.freezeRow().isEnabled).isFalse()
            assertThat(sentMessages.filterIsInstance<BottomSheetMessage>()).isEmpty()
            coVerify(exactly = 0) { changeCardFrozenStateUseCase(any(), any(), any()) }
        }

    @Test
    fun `GIVEN freeze confirmed WHEN operation is in progress THEN freeze row is loading until it ends`() = runTest {
        // Arrange
        val result = CompletableDeferred<Either<UniversalError, Unit>>()
        coEvery { changeCardFrozenStateUseCase(any(), any(), any()) } coAnswers { result.await() }
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()

        // Act
        model.confirmFreezeOfSelectedCard()
        runCurrent()
        val isLoadingDuringOperation = model.freezeRow().isLoading
        result.complete(Unit.right())
        runCurrent()

        // Assert
        assertThat(isLoadingDuringOperation to model.freezeRow().isLoading).isEqualTo(true to false)
        coVerify(exactly = 1) { changeCardFrozenStateUseCase(userWalletId, firstCard, true) }
    }

    @Test
    fun `GIVEN first card freezing WHEN second card frozen THEN both run and each finishes on its own`() = runTest {
        // Arrange
        val firstResult = CompletableDeferred<Either<UniversalError, Unit>>()
        val secondResult = CompletableDeferred<Either<UniversalError, Unit>>()
        coEvery { changeCardFrozenStateUseCase(any(), firstCard, any()) } coAnswers { firstResult.await() }
        coEvery { changeCardFrozenStateUseCase(any(), secondCard, any()) } coAnswers { secondResult.await() }
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()
        model.confirmFreezeOfSelectedCard()
        runCurrent()

        // Act
        model.onCardPageSelected(index = 1)
        runCurrent()
        model.confirmFreezeOfSelectedCard()
        runCurrent()
        firstResult.complete(Unit.right())
        runCurrent()

        // Assert
        assertThat(model.freezeRow().isLoading).isTrue()
        coVerify(exactly = 1) { changeCardFrozenStateUseCase(userWalletId, firstCard, true) }
        coVerify(exactly = 1) { changeCardFrozenStateUseCase(userWalletId, secondCard, true) }
        secondResult.complete(Unit.right())
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

    @Test
    fun `GIVEN second card selected WHEN details row clicked THEN only its controller requests reveal`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()
        model.onCardPageSelected(index = 1)
        runCurrent()

        // Act
        model.uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Details }.onClick()

        // Assert
        verify(exactly = 1) { cardControllers.getValue(secondCard.id).requestReveal() }
        verify(exactly = 0) { cardControllers.getValue(firstCard.id).requestReveal() }
    }

    @Test
    fun `GIVEN banking details failed to load WHEN add funds reports it THEN details error sheet opens`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // Act
        model.onBankingDetailsLoadFailed(PRODUCT_INSTANCE_ID)

        // Assert
        assertThat(openedSheets.filterIsInstance<TangemPayCardNavigation.VaBankingDetailsError>()).containsExactly(
            TangemPayCardNavigation.VaBankingDetailsError(productInstanceId = PRODUCT_INSTANCE_ID),
        )
    }

    @Test
    fun `GIVEN credentials load WHEN banking details retried THEN account details screen opens`() = runTest {
        // Arrange
        coEvery { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) } returns BANK_CREDENTIALS.right()
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()

        // Act
        val actual = model.retryBankCredentials(productInstanceId = PRODUCT_INSTANCE_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        verify(exactly = 1) {
            router.push(TangemPayAccountDetailsInnerRoute.BankTransferOnrampDetails(BANK_CREDENTIALS), any())
        }
    }

    @Test
    fun `GIVEN credentials fail WHEN banking details retried THEN error returned and nothing opened`() = runTest {
        // Arrange
        coEvery { getBankCredentialsUseCase(userWalletId, PRODUCT_INSTANCE_ID) } returns
            TangemPayApiError.Unspecified.left()
        val model = createModel()
        statusFlow.emit(paymentStatus)
        runCurrent()

        // Act
        val actual = model.retryBankCredentials(productInstanceId = PRODUCT_INSTANCE_ID)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.Unspecified.left())
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN card actions disabled WHEN actual status emitted THEN top bar menu kept and actions menu absent`() =
        runTest {
            // Arrange
            val model = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Assert
            assertThat(model.cardActionsLayout()).isEqualTo(
                CardActionsLayout(
                    settings = listOf(
                        SettingState(TangemPayCardPageSetting.Id.Details),
                        SettingState(TangemPayCardPageSetting.Id.Freeze),
                        SettingState(TangemPayCardPageSetting.Id.ChangePin),
                    ),
                    menuItemTitles = listOf(REISSUE_TITLE, CLOSE_TITLE),
                    actionsMenu = null,
                ),
            )
            assertThat(model.uiState.value.payMenu).isNull()
            verify(exactly = 0) { closeCoordinator.closedCards }
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
            AddToWalletBlocksModel(
                entry = GoogleWalletEntry.Added,
                expected = AddToWalletBlocks(isBannerShown = false, button = null, isAddedStatusShown = true),
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
                isAddedStatusShown = state.isAddedToGoogleWallet,
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MenuItems {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN selected card state WHEN actual status emitted THEN menu items follow card activation`(
            model: MenuItemsModel,
        ) = runTest {
            // Arrange
            every { loadedStatus.cards } returns listOf(firstCard.copy(state = model.cardState), secondCard)
            val pageModel = createModel()

            // Act
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Assert
            assertThat(pageModel.menuItemTitles()).isEqualTo(model.expected)
            pageModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            MenuItemsModel(cardState = TangemPayCardState.Active, expected = listOf(REISSUE_TITLE, CLOSE_TITLE)),
            MenuItemsModel(cardState = TangemPayCardState.Delivering, expected = emptyList()),
            MenuItemsModel(cardState = TangemPayCardState.Activating, expected = emptyList()),
        )

        @Test
        fun `GIVEN cached status with card awaiting activation WHEN model created THEN menu is empty`() = runTest {
            // Arrange
            val cachedStatus: AccountStatus.Payment = mockk(relaxed = true) {
                every { account } returns Account.Payment(userWalletId)
                every { value } returns mockk<PaymentAccountStatusValue.Loaded>(relaxed = true) {
                    every { source } returns StatusSource.ONLY_CACHE
                    every { cards } returns listOf(firstCard.copy(state = TangemPayCardState.Delivering), secondCard)
                }
            }

            // Act
            val pageModel = createModel(initialStatus = cachedStatus)

            // Assert
            assertThat(pageModel.menuItemTitles()).isEmpty()
            pageModel.onDestroy()
        }

        private fun TangemPayCardPageModel.menuItemTitles(): List<TextReference> =
            uiState.value.menuItems.map(TangemPayDropDownItemUM::title)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardActionsMenu {

        @BeforeEach
        fun setUp() {
            enableCardActions()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card actions enabled WHEN actual status emitted THEN settings and actions menu follow the cards`(
            model: CardActionsLayoutModel,
        ) = runTest {
            // Arrange
            every { loadedStatus.cards } returns model.cards
            val pageModel = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Assert
            assertThat(pageModel.cardActionsLayout()).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CardActionsLayoutModel(
                cards = listOf(firstCard, secondCard),
                expected = cardActionsLayout(actionsMenu(FREEZE_ITEM, REPLACE_ITEM, DIVIDER, CLOSE_ITEM)),
            ),
            CardActionsLayoutModel(
                cards = listOf(firstCard),
                expected = cardActionsLayout(actionsMenu(FREEZE_ITEM, REPLACE_ITEM, DIVIDER, LAST_CARD_CLOSE_ITEM)),
            ),
            CardActionsLayoutModel(
                cards = listOf(firstCard.copy(frozenState = TangemPayCardFrozenState.Frozen), secondCard),
                expected = cardActionsLayout(
                    actionsMenu = actionsMenu(UNFREEZE_ITEM, REPLACE_ITEM, DIVIDER, CLOSE_ITEM),
                    settings = listOf(
                        SettingState(TangemPayCardPageSetting.Id.Details, isEnabled = false),
                        SettingState(TangemPayCardPageSetting.Id.ChangePin, isEnabled = false),
                        SettingState(TangemPayCardPageSetting.Id.Actions),
                    ),
                ),
            ),
            CardActionsLayoutModel(
                cards = listOf(firstCard.copy(frozenState = TangemPayCardFrozenState.Pending), secondCard),
                expected = cardActionsLayout(
                    actionsMenu = actionsMenu(FREEZE_ITEM.copy(isEnabled = false), REPLACE_ITEM, DIVIDER, CLOSE_ITEM),
                    settings = listOf(
                        SettingState(TangemPayCardPageSetting.Id.Details, isEnabled = false),
                        SettingState(TangemPayCardPageSetting.Id.ChangePin, isEnabled = false),
                        SettingState(TangemPayCardPageSetting.Id.Actions, isLoading = true),
                    ),
                ),
            ),
            CardActionsLayoutModel(
                cards = listOf(firstCard.copy(state = TangemPayCardState.Delivering), secondCard),
                expected = cardActionsLayout(actionsMenu(FREEZE_ITEM)),
            ),
        )

        @Test
        fun `GIVEN last active card WHEN actions opened twice THEN close unavailable reported once`() = runTest {
            // Arrange
            every { loadedStatus.cards } returns listOf(firstCard)
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Act
            repeat(times = 2) { pageModel.actionsSetting().onClick() }

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.CloseCardUnavailableShown()) }
        }

        @Test
        fun `GIVEN several active cards WHEN actions opened THEN close unavailable not reported`() = runTest {
            // Arrange
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Act
            pageModel.actionsSetting().onClick()

            // Assert
            verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.CloseCardUnavailableShown>()) }
        }

        @Test
        fun `GIVEN named card WHEN close item clicked THEN close confirmation opens for the selected card`() = runTest {
            // Arrange
            val namedCard = firstCard.copy(displayName = CardDisplayName("Travel").getOrNull())
            every { loadedStatus.cards } returns listOf(namedCard, secondCard)
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()
            val openedSheets = pageModel.bottomSheetNavigation.trackSlot()

            // Act
            pageModel.actionsMenuItem(R.string.tangempay_card_close).onClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(
                    TangemPayAnalyticsEvents.CloseCardClicked(
                        TangemPayAnalyticsEvents.CloseCardClicked.Source.ActionsMenu,
                    ),
                )
            }
            assertThat(openedSheets).containsExactly(
                TangemPayCardNavigation.CloseCardConfirm(
                    userWalletId = userWalletId,
                    cardId = firstCard.id,
                    customerId = CUSTOMER_ID,
                    cardName = "Travel",
                    lastDigits = firstCard.lastDigits,
                    canFreeze = true,
                ),
            )
        }

        @Test
        fun `GIVEN close confirmation WHEN freeze chosen instead THEN card frozen without freeze confirmation`() =
            runTest {
                // Arrange
                coEvery { changeCardFrozenStateUseCase(any(), any(), any()) } returns Unit.right()
                val pageModel = createModel().destroyAfterTest()
                statusFlow.emit(paymentStatus)
                runCurrent()

                // Act
                pageModel.onFreezeInsteadOfClose()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { changeCardFrozenStateUseCase(userWalletId, firstCard, true) }
                assertThat(sentMessages).containsExactly(
                    SnackbarMessage(message = resourceReference(R.string.tangem_pay_freeze_card_success)),
                )
            }

        @Test
        fun `GIVEN selected card frozen state Pending WHEN onFreezeInsteadOfClose THEN freeze use case not called and no snackbar`() =
            runTest {
                // Arrange
                coEvery { changeCardFrozenStateUseCase(any(), any(), any()) } returns Unit.right()
                every { loadedStatus.cards } returns listOf(
                    firstCard.copy(frozenState = TangemPayCardFrozenState.Pending),
                    secondCard,
                )
                val pageModel = createModel().destroyAfterTest()
                statusFlow.emit(paymentStatus)
                runCurrent()

                // Act
                pageModel.onFreezeInsteadOfClose()
                runCurrent()

                // Assert
                coVerify(exactly = 0) { changeCardFrozenStateUseCase(any(), any(), any()) }
                assertThat(sentMessages).isEmpty()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClosedCards {

        @BeforeEach
        fun setUp() {
            enableCardActions()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card actions enabled WHEN card closed THEN stack pops to Pay main only for its own selected card`(
            model: ClosedCardModel,
        ) = runTest {
            // Arrange
            createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Act
            closedCardsFlow.emit(model.closedCard)
            runCurrent()

            // Assert
            verify(exactly = model.expectedPops) { router.popTo(PAY_MAIN_ROUTE, any()) }
            verify(exactly = 0) { router.pop(any()) }
        }

        private fun provideTestModels() = listOf(
            ClosedCardModel(closedCard = TangemPayClosedCard(userWalletId, firstCard.id), expectedPops = 1),
            ClosedCardModel(closedCard = TangemPayClosedCard(OTHER_USER_WALLET_ID, firstCard.id), expectedPops = 0),
            ClosedCardModel(closedCard = TangemPayClosedCard(userWalletId, secondCard.id), expectedPops = 0),
        )

        @Test
        fun `GIVEN selected card dropped by status refresh WHEN it is reported closed THEN stack pops to Pay main`() =
            runTest {
                // Arrange
                createModel().destroyAfterTest()
                statusFlow.emit(paymentStatus)
                runCurrent()
                every { loadedStatus.cards } returns listOf(secondCard)
                statusFlow.emit(paymentStatus)
                runCurrent()

                // Act
                closedCardsFlow.emit(TangemPayClosedCard(userWalletId, firstCard.id))
                runCurrent()

                // Assert
                verify(exactly = 1) { router.popTo(PAY_MAIN_ROUTE, any()) }
                verify(exactly = 0) { router.pop(any()) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PayMenu {

        @BeforeEach
        fun setUp() {
            enableCardActions()
        }

        @ParameterizedTest
        @MethodSource("cashbackMenuModels")
        fun `GIVEN cashback summary WHEN actual status emitted THEN cashback shown only while enabled`(
            model: CashbackMenuModel,
        ) = runTest {
            // Arrange
            val pageModel = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            cashbackSummaryFlow.emit(model.summary)
            runCurrent()

            // Assert
            assertThat(pageModel.payMenuWithoutClicks()).isEqualTo(model.expected)
        }

        private fun cashbackMenuModels() = listOf(
            CashbackMenuModel(
                summary = CashbackSummary.Enabled(displayMode = CashbackDisplayMode.FULL, cashback = CASHBACK),
                expected = menuOf(PAY_CASHBACK_ITEM, PAY_SUPPORT_ITEM),
            ),
            CashbackMenuModel(
                summary = CashbackSummary.Enabled(
                    displayMode = CashbackDisplayMode.ALT_BLOCK,
                    cashback = CASHBACK.copy(confirmedAmount = BigDecimal.ZERO),
                ),
                expected = menuOf(PAY_CASHBACK_ITEM, PAY_SUPPORT_ITEM),
            ),
            CashbackMenuModel(
                summary = CashbackSummary.Enabled(displayMode = CashbackDisplayMode.ALT_BLOCK, cashback = CASHBACK),
                expected = menuOf(PAY_CASHBACK_ITEM, PAY_SUPPORT_ITEM),
            ),
            CashbackMenuModel(summary = CashbackSummary.Deactivated, expected = menuOf(PAY_SUPPORT_ITEM)),
            CashbackMenuModel(summary = CashbackSummary.Disabled, expected = menuOf(PAY_SUPPORT_ITEM)),
            CashbackMenuModel(summary = CashbackSummary.Unknown, expected = menuOf(PAY_SUPPORT_ITEM)),
            CashbackMenuModel(summary = null, expected = menuOf(PAY_SUPPORT_ITEM)),
        )

        @ParameterizedTest
        @MethodSource("planMenuModels")
        fun `GIVEN tariff plan and no cached cashback WHEN actual status emitted THEN pay menu starts with the plan`(
            model: PlanMenuModel,
        ) = runTest {
            // Arrange
            every { loadedStatus.tariffPlan } returns model.tariffPlan
            val pageModel = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Assert
            assertThat(pageModel.payMenuWithoutClicks()).isEqualTo(model.expected)
        }

        private fun planMenuModels(): List<PlanMenuModel> {
            val plan = tariffPlanState()
            val changingPlan = tariffPlanState(order = awaitingDepositOrder())
            return listOf(
                PlanMenuModel(
                    tariffPlan = plan,
                    expected = menuOf(
                        PAY_CHANGE_PLAN_ITEM.copy(description = planMenuSubtitle(plan)),
                        PAY_SUPPORT_ITEM,
                    ),
                ),
                PlanMenuModel(
                    tariffPlan = changingPlan,
                    expected = menuOf(
                        PAY_CHANGE_PLAN_ITEM.copy(
                            description = resourceReference(R.string.tangempay_changing_plan),
                            iconStart = Icons.ic_arrow_refresh_20,
                        ),
                        PAY_SUPPORT_ITEM,
                    ),
                ),
                PlanMenuModel(tariffPlan = null, expected = menuOf(PAY_SUPPORT_ITEM)),
            )
        }

        @Test
        fun `GIVEN plan and enabled cashback WHEN actual status emitted THEN pay menu is plan cashback support`() =
            runTest {
                // Arrange
                val plan = tariffPlanState()
                every { loadedStatus.tariffPlan } returns plan
                val pageModel = createModel().destroyAfterTest()

                // Act
                statusFlow.emit(paymentStatus)
                cashbackSummaryFlow.emit(
                    CashbackSummary.Enabled(displayMode = CashbackDisplayMode.FULL, cashback = CASHBACK),
                )
                runCurrent()

                // Assert
                assertThat(pageModel.payMenuWithoutClicks()).isEqualTo(
                    menuOf(
                        PAY_CHANGE_PLAN_ITEM.copy(description = planMenuSubtitle(plan)),
                        PAY_CASHBACK_ITEM,
                        PAY_SUPPORT_ITEM,
                    ),
                )
            }

        @Test
        fun `GIVEN card awaiting activation WHEN actual status emitted THEN pay menu shown`() = runTest {
            // Arrange
            every { loadedStatus.cards } returns listOf(firstCard.copy(state = TangemPayCardState.Delivering), secondCard)
            val pageModel = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Assert
            assertThat(pageModel.payMenuWithoutClicks()).isEqualTo(menuOf(PAY_SUPPORT_ITEM))
        }

        @Test
        fun `GIVEN change plan item WHEN clicked THEN current plan opened and click reported`() = runTest {
            // Arrange
            val plan = tariffPlanState()
            every { loadedStatus.tariffPlan } returns plan
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Act
            pageModel.payMenuItem(R.string.tangempay_current_plan_change).onClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Tiers.CurrentPlanClicked()) }
            verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.CurrentPlan(plan), any()) }
        }

        @Test
        fun `GIVEN cashback item WHEN clicked THEN cashback opened and click reported`() = runTest {
            // Arrange
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            cashbackSummaryFlow.emit(CashbackSummary.Enabled(displayMode = CashbackDisplayMode.FULL, cashback = CASHBACK))
            runCurrent()

            // Act
            pageModel.payMenuItem(R.string.tangempay_cashback_title).onClick()

            // Assert
            verify(exactly = 1) { analytics.send(TangemPayAnalyticsEvents.Cashback.ButtonInSettingsClicked()) }
            verify(exactly = 1) { router.push(TangemPayAccountDetailsInnerRoute.Cashback, any()) }
        }

        @Test
        fun `GIVEN support item WHEN clicked THEN support click reported`() = runTest {
            // Arrange
            val pageModel = createModel().destroyAfterTest()
            statusFlow.emit(paymentStatus)
            runCurrent()

            // Act
            pageModel.payMenuItem(R.string.tangempay_pay_support).onClick()

            // Assert
            verify(exactly = 1) {
                analytics.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
            }
        }

        @Test
        fun `GIVEN pay menu shown WHEN model created THEN no cashback impressions sent`() = runTest {
            // Arrange
            val pageModel = createModel().destroyAfterTest()

            // Act
            statusFlow.emit(paymentStatus)
            cashbackSummaryFlow.emit(CashbackSummary.Enabled(displayMode = CashbackDisplayMode.FULL, cashback = CASHBACK))
            runCurrent()

            // Assert
            assertThat(pageModel.uiState.value.payMenu).isNotNull()
            verify(exactly = 0) { analytics.send(match { it.category == "Visa Cashback" }) }
        }

        private fun TangemPayCardPageModel.payMenuWithoutClicks(): TangemMenu.UiState? =
            uiState.value.payMenu?.let { menu -> menu.copy(entries = menu.entries.map { it.withoutClick() }.toPersistentList()) }

        private fun TangemPayCardPageModel.payMenuItem(label: Int): TangemMenu.UiState.Item =
            checkNotNull(uiState.value.payMenu).entries
                .filterIsInstance<TangemMenu.UiState.Item>()
                .first { it.label == resourceReference(label) }

        private fun planMenuSubtitle(plan: TangemPayTariffPlanState): TextReference =
            resourceReference(R.string.tangempay_plan_menu_subtitle, wrappedList(plan.tariff.plan.name))
    }

    internal data class CashbackMenuModel(val summary: CashbackSummary?, val expected: TangemMenu.UiState)

    internal data class PlanMenuModel(val tariffPlan: TangemPayTariffPlanState?, val expected: TangemMenu.UiState)

    internal data class MenuItemsModel(val cardState: TangemPayCardState, val expected: List<TextReference>)

    internal data class AddToWalletBlocksModel(val entry: GoogleWalletEntry, val expected: AddToWalletBlocks)

    internal data class AddToWalletBlocks(
        val isBannerShown: Boolean,
        val button: ButtonState?,
        val isAddedStatusShown: Boolean = false,
    )

    internal data class ButtonState(val isLoading: Boolean)

    internal data class CardActionsLayoutModel(val cards: List<TangemPayCard>, val expected: CardActionsLayout)

    internal data class CardActionsLayout(
        val settings: List<SettingState>,
        val menuItemTitles: List<TextReference>,
        val actionsMenu: TangemMenu.UiState?,
    )

    internal data class SettingState(
        val id: TangemPayCardPageSetting.Id,
        val isEnabled: Boolean = true,
        val isLoading: Boolean = false,
    )

    internal data class ClosedCardModel(val closedCard: TangemPayClosedCard, val expectedPops: Int)

    private fun enableCardActions() {
        clearMocks(analytics, router, changeCardFrozenStateUseCase)
        clearMocks(uiMessageSender, answers = false)
        sentMessages.clear()
        statusFlow.resetReplayCache()
        cashbackSummaryFlow.resetReplayCache()
        every { tangemPayFeatureToggles.isCardActionsEnabled } returns true
        every { loadedStatus.cards } returns listOf(firstCard, secondCard)
        every { loadedStatus.customerId } returns CUSTOMER_ID
        every { loadedStatus.tariffPlan } returns null
    }

    private fun menuOf(vararg entries: TangemMenu.UiState.Entry) =
        TangemMenu.UiState(entries = persistentListOf(*entries))

    private fun cardActionsLayout(
        actionsMenu: TangemMenu.UiState,
        settings: List<SettingState> = listOf(
            SettingState(TangemPayCardPageSetting.Id.Details),
            SettingState(TangemPayCardPageSetting.Id.ChangePin),
            SettingState(TangemPayCardPageSetting.Id.Actions),
        ),
    ) = CardActionsLayout(settings = settings, menuItemTitles = emptyList(), actionsMenu = actionsMenu)

    private fun actionsMenu(vararg entries: TangemMenu.UiState.Entry) =
        TangemMenu.UiState(entries = persistentListOf(*entries))

    private fun TangemPayCardPageModel.cardActionsLayout(): CardActionsLayout = uiState.value.let { state ->
        CardActionsLayout(
            settings = state.settings.map {
                SettingState(id = it.id, isEnabled = it.isEnabled, isLoading = it.isLoading)
            },
            menuItemTitles = state.menuItems.map(TangemPayDropDownItemUM::title),
            actionsMenu = state.actionsMenu?.let { menu ->
                menu.copy(entries = menu.entries.map { it.withoutClick() }.toPersistentList())
            },
        )
    }

    private fun TangemMenu.UiState.Entry.withoutClick(): TangemMenu.UiState.Entry =
        if (this is TangemMenu.UiState.Item) copy(onClick = NO_OP_CLICK) else this

    private fun TangemPayCardPageModel.actionsSetting(): TangemPayCardPageSetting =
        uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Actions }

    private fun TangemPayCardPageModel.actionsMenuItem(label: Int): TangemMenu.UiState.Item =
        checkNotNull(uiState.value.actionsMenu).entries
            .filterIsInstance<TangemMenu.UiState.Item>()
            .first { it.label == resourceReference(label) }

    private fun TangemPayCardPageModel.confirmFreezeOfSelectedCard() {
        freezeRow().onClick()
        val sheet = sentMessages.filterIsInstance<BottomSheetMessage>().last()
        sheet.messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.Button>()
            .last()
            .onClick
            ?.invoke(sheet.messageBottomSheetUM.closeScope)
    }

    private fun TangemPayCardPageModel.freezeRow(): TangemPayCardPageSetting =
        uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Freeze }

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
        router = router,
        analytics = analytics,
        sendFeedbackEmailUseCase = mockk(relaxed = true),
        cardDetailsRepository = cardDetailsRepository,
        uiMessageSender = uiMessageSender,
        changeCardFrozenStateUseCase = changeCardFrozenStateUseCase,
        cardDetailsEventListener = eventListener,
        cardDetailsControllerFactory = controllerFactory,
        cardAuthenticator = mockk(relaxed = true),
        onboardingRepository = mockk(relaxed = true),
        getBankCredentialsUseCase = getBankCredentialsUseCase,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
        closeCoordinator = closeCoordinator,
        observeCashbackSummaryUseCase = observeCashbackSummaryUseCase,
        googleWalletControllerFactory = googleWalletControllerFactory,
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi_account"

        val BANK_CREDENTIALS = bankCredentials()
        val PAY_MAIN_ROUTE = TangemPayAccountDetailsInnerRoute.AccountDetails
        val REISSUE_TITLE: TextReference = TextReference.Res(R.string.tangempay_card_details_reissue_card)
        val CLOSE_TITLE: TextReference = TextReference.Res(R.string.tangem_pay_close_card_popup_primary_button_title)

        const val CUSTOMER_ID = "customer_1"
        val OTHER_USER_WALLET_ID = UserWalletId("456")

        val NO_OP_CLICK: () -> Unit = {}
        val FREEZE_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangem_pay_freeze_card_freeze),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_snowflake_20,
        )
        val UNFREEZE_ITEM = FREEZE_ITEM.copy(label = resourceReference(R.string.tangem_pay_freeze_card_unfreeze))
        val REPLACE_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_card_replace),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_arrow_refresh_20,
        )
        val DIVIDER = TangemMenu.UiState.Divider()
        val CLOSE_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_card_close),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_trash_bin_20,
            variant = TangemMenu.ItemVariant.Error,
        )
        val LAST_CARD_CLOSE_ITEM = CLOSE_ITEM.copy(
            description = resourceReference(R.string.tangempay_card_close_last_card_disabled),
            isEnabled = false,
        )

        val PAY_CHANGE_PLAN_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_current_plan_change),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_info_20,
        )
        val PAY_CASHBACK_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_cashback_title),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_percent_backward_20,
        )
        val PAY_SUPPORT_ITEM = TangemMenu.UiState.Item(
            label = resourceReference(R.string.tangempay_pay_support),
            onClick = NO_OP_CLICK,
            iconStart = Icons.ic_mail_20,
        )
        val CASHBACK = TangemPayCashback(
            confirmedAmount = BigDecimal("32.15"),
            totalEarnedAmount = BigDecimal("132.15"),
            currency = "USD",
            payoutCurrency = "USDC",
            period = TangemPayCashback.Period(year = 2026, month = 6, payoutStart = null, payoutEnd = null),
            previousPayout = null,
        )
    }
}