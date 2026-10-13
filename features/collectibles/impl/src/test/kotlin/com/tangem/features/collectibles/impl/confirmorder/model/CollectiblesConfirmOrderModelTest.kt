package com.tangem.features.collectibles.impl.confirmorder.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.domain.collectibles.usecase.GetCollectibleGachaMachineUseCase
import com.tangem.domain.collectibles.usecase.OpenCollectiblePackUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.confirmorder.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.confirmorder.CollectiblesConfirmOrderComponent
import com.tangem.features.collectibles.impl.confirmorder.ui.state.CollectiblesConfirmOrderUM
import com.tangem.features.collectibles.impl.createMachine
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesConfirmOrderModelTest {

    private val getMachineUseCase: GetCollectibleGachaMachineUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val accountStatusSupplier: CollectiblesAccountStatusSupplier = mockk()
    private val openCollectiblePackUseCase: OpenCollectiblePackUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private val userWallet = MockUserWalletFactory.create().copy(walletId = USER_WALLET_ID)

    private val statusFlow = MutableStateFlow<CollectiblesAccountStatusValue>(onboarded(usdcAmount = "1000"))

    private var model: CollectiblesConfirmOrderModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(
            getMachineUseCase,
            router,
            accountStatusSupplier,
            openCollectiblePackUseCase,
            userWalletsListRepository,
            messageSender,
        )
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { openCollectiblePackUseCase(any(), any()) } returns flowOf(OpenPackStep.Opened(PACK))
        statusFlow.value = onboarded(usdcAmount = "1000")
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
        assertThat(state).isInstanceOf(CollectiblesConfirmOrderUM.Content::class.java)

        val content = state as CollectiblesConfirmOrderUM.Content
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
        assertThat(model.uiState.value).isInstanceOf(CollectiblesConfirmOrderUM.Error::class.java)
    }

    @Test
    fun `GIVEN the machine failed WHEN reload tapped twice THEN one cache-bypassing retry runs with a spinner`() =
        runTest {
            // Arrange
            coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
            coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { CompletableDeferred<MachineResult>().await() }
            val model = startModel()
            val error = model.uiState.value as CollectiblesConfirmOrderUM.Error

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
        (model.uiState.value as CollectiblesConfirmOrderUM.Error).onReloadClick()
        runCurrent()

        // Act
        retry.complete(createMachine().right())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(CollectiblesConfirmOrderUM.Content::class.java)
    }

    @Test
    fun `GIVEN a retry in flight WHEN it fails again THEN the error is shown without the spinner`() = runTest {
        // Arrange
        val retry = CompletableDeferred<MachineResult>()
        coEvery { getMachineUseCase(EXTERNAL_ID, false) } returns CollectiblesError.Api(IllegalStateException()).left()
        coEvery { getMachineUseCase(EXTERNAL_ID, true) } coAnswers { retry.await() }
        val model = startModel()
        val error = model.uiState.value as CollectiblesConfirmOrderUM.Error
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
        assertThat(model.content().isPackOpened).isTrue()
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
            CollectiblesConfirmOrderUM.ButtonUM(
                textRef = resourceReference(R.string.collectibles_confirm_order_confirming_transaction_button),
                isEnabled = false,
            ),
        )
        assertThat(model.content().isPackOpened).isFalse()
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
        every { openCollectiblePackUseCase(any(), any()) } returns
            flowOf(OpenPackStep.Failed.PurchaseFailed)
        val message = slot<EventMessage>()
        every { messageSender.send(capture(message)) } returns Unit
        val model = startModel()

        // Act
        model.content().onApproveClick()
        advanceUntilIdle()

        // Assert
        assertThat(message.captured).isInstanceOf(SnackbarMessage::class.java)
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
        assertThat(model.content().isPackOpened).isTrue()
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

    private fun CollectiblesConfirmOrderModel.content() = uiState.value as CollectiblesConfirmOrderUM.Content

    private fun CollectiblesConfirmOrderModel.order() = content().order

    private fun onboarded(usdcAmount: String) = CollectiblesAccountStatusValue.Onboarded(
        source = StatusSource.ACTUAL,
        vaultBalance = MockCollectibles.vaultBalance(amount = BigDecimal(usdcAmount), fiatRate = BigDecimal.ONE),
    )

    private fun TestScope.startModel(): CollectiblesConfirmOrderModel {
        val model = CollectiblesConfirmOrderModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            getMachineUseCase = getMachineUseCase,
            accountStatusSupplier = accountStatusSupplier,
            openCollectiblePackUseCase = openCollectiblePackUseCase,
            userWalletsListRepository = userWalletsListRepository,
            messageSender = messageSender,
            router = router,
            paramsContainer = MutableParamsContainer(
                CollectiblesConfirmOrderComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    externalId = EXTERNAL_ID,
                    isTurboEnabled = false,
                ),
            ),
        ).also { this@CollectiblesConfirmOrderModelTest.model = it }

        advanceUntilIdle()

        return model
    }

    private companion object {

        const val TX_ID = "tx-id"

        const val EXTERNAL_ID = "pokemon_1"
        val USER_WALLET_ID = UserWalletId("011")

        val PACK = OpenedCollectiblePack(
            txId = "tx-id",
            nftAddress = "nft",
            imageFrontUrl = null,
            imageBackUrl = null,
            rarity = null,
            gradingCompany = null,
            gradingId = "123",
            year = null,
            set = null,
            serialNumber = null,
            insuredValue = null,
        )
    }
}

private typealias MachineResult = Either<CollectiblesError, CollectibleGachaMachine>